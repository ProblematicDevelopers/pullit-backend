package com.pullit.filehistory.service;

import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.common.s3.enums.S3Directory;
import com.pullit.common.s3.service.S3Service;
import com.pullit.filehistory.dto.response.PdfProcessingResponse;
import com.pullit.filehistory.entity.FileHistory;
import com.pullit.filehistory.entity.PdfImage;
import com.pullit.filehistory.repository.FileHistoryRepository;
import com.pullit.filehistory.repository.PdfImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfProcessingServiceImpl implements PdfProcessingService {

    private final S3Service s3Service;
    private final FileHistoryRepository fileHistoryRepository;
    private final PdfImageRepository pdfImageRepository;

    @Value("${app.pdf.image-dpi:300}")
    private int imageDpi;

    @Value("${app.pdf.image-format:png}")
    private String imageFormat;

    @Override
    @Transactional
    public PdfProcessingResponse processPdfToImages(MultipartFile pdfFile, Long fileHistoryId) throws BusinessException {
        log.info("Starting PDF processing: fileHistoryId={}, fileName={}", fileHistoryId, pdfFile.getOriginalFilename());

        try {
            FileHistory fileHistory = fileHistoryRepository.findById(fileHistoryId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

            List<PdfProcessingResponse.PdfImageInfo> imageInfos = new ArrayList<>();

            byte[] pdfBytes = pdfFile.getBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                int pageCount = document.getNumberOfPages();

                log.info("PDF loaded successfully. Total pages: {}", pageCount);

                for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                    float scale = imageDpi / 72f; // 72 DPI 기본
                    BufferedImage bufferedImage = pdfRenderer.renderImage(pageIndex, scale);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bufferedImage, imageFormat, baos);
                    byte[] imageBytes = baos.toByteArray();

                    String fileName = String.format("page_%d_%d.%s", fileHistoryId, pageIndex + 1, imageFormat);
                    var uploadResponse = s3Service.upload(imageBytes, fileName, S3Directory.IMAGE_QUESTION);
                    String s3Key = uploadResponse.getS3Key();

                    // DB에는 s3Key 중심 저장 (imageUrl 저장 불필요)
                    PdfImage pdfImage = PdfImage.builder()
                            .fileHistory(fileHistory)
                            .pageNumber(pageIndex + 1)
                            .imageUrl(null)
                            .imageWidth(bufferedImage.getWidth())
                            .imageHeight(bufferedImage.getHeight())
                            .fileSize((long) imageBytes.length)
                            .s3Key(s3Key)
                            .build();

                    pdfImage = pdfImageRepository.save(pdfImage);

                    // 응답에는 fresh presigned(1h)
                    String freshUrl = s3Service.generatePresignedUrl(s3Key, Duration.ofHours(1));

                    PdfProcessingResponse.PdfImageInfo imageInfo = PdfProcessingResponse.PdfImageInfo.builder()
                            .pdfImageId(pdfImage.getId())
                            .pageNumber(pageIndex + 1)
                            .imageUrl(freshUrl)
                            .width(bufferedImage.getWidth())
                            .height(bufferedImage.getHeight())
                            .fileSize((long) imageBytes.length)
                            .build();

                    imageInfos.add(imageInfo);

                    log.debug("Page {} processed successfully. Image size: {}x{}, File size: {} bytes",
                            pageIndex + 1, bufferedImage.getWidth(), bufferedImage.getHeight(), imageBytes.length);
                }

                // ✅ 초기 imgOrder를 0..n-1 로 저장 (FE가 안보내도 일관 동작)
                fileHistory.setImgOrder(buildSequentialOrder(pageCount));
                fileHistory.setImgCount(pageCount);
                fileHistoryRepository.save(fileHistory);

                log.info("PDF processing completed successfully. Total pages processed: {}", pageCount);

                return PdfProcessingResponse.builder()
                        .fileHistoryId(fileHistoryId)
                        .totalPages(pageCount)
                        .images(imageInfos)
                        .message("PDF processing completed successfully")
                        .build();
            }
        } catch (IOException e) {
            log.error("Error processing PDF file: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_PROCESSING_ERROR);
        }
    }

    // ---------- 정렬/정규화 유틸 ----------

    private String buildSequentialOrder(int n) {
        return java.util.stream.IntStream.range(0, n)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /** "3,5,2" / "1,3,2" 모두 허용 → 0-based 고유 리스트로 정규화 후 누락 보충 */
    private List<Integer> normalizeZeroBased(String raw, int n) {
        if (raw == null || raw.isBlank() || n <= 0) {
            return java.util.stream.IntStream.range(0, n).boxed().collect(Collectors.toList());
        }
        // parse
        List<Integer> parsed = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try { return Integer.parseInt(s); } catch (Exception e) { return null; }
                })
                .collect(Collectors.toList());

        // 1-based→0-based, 0은 유지, 음수/범위밖 제거, 중복 제거
        LinkedHashSet<Integer> set = new LinkedHashSet<>();
        for (Integer i : parsed) {
            if (i == null) continue;
            int z = (i > 0) ? (i - 1) : i; // 1→0 변환, 0은 그대로
            if (z >= 0 && z < n) set.add(z);
        }
        // 누락 보충
        for (int i = 0; i < n; i++) set.add(i);

        return new ArrayList<>(set).subList(0, n);
    }


    // 0-based 퍼뮤테이션 검증/파싱: 길이 n, 값 0..n-1, 중복 없음이면 그대로 반환, 아니면 null
    private List<Integer> parseZeroBasedPermutation(String raw, int n) {
        if (raw == null) return null;
        String[] parts = raw.split(",");
        if (parts.length != n) return null;

        boolean[] seen = new boolean[n];
        List<Integer> result = new ArrayList<>(n);
        for (String s : parts) {
            s = s.trim();
            if (s.isEmpty()) return null;
            int v;
            try { v = Integer.parseInt(s); } catch (Exception e) { return null; }
            if (v < 0 || v >= n || seen[v]) return null;
            seen[v] = true;
            result.add(v);
        }
        return result;
    }

    private List<PdfImage> reorderByImgOrder(List<PdfImage> source, String imgOrder) {
        if (source == null || source.isEmpty()) return source;

        // pageNumber 오름차순이 '원본 인덱스' 0..n-1에 대응
        List<PdfImage> byPage = new ArrayList<>(source);
        byPage.sort(Comparator.comparing(PdfImage::getPageNumber));

        List<Integer> perm = parseZeroBasedPermutation(imgOrder, byPage.size());
        if (perm == null) {
            // 퍼뮤테이션이 아니면 원본 순서 유지(또는 여기서 예외 던져도 됨)
            log.warn("imgOrder is not a valid zero-based permutation. order={}", imgOrder);
            return byPage;
        }
        return perm.stream().map(byPage::get).collect(Collectors.toList());
    }

    // ---------- API 구현 ----------

    @Override
    @Transactional
    public List<String> updateImageOrder(Long fileHistoryId, String imageOrder) {
        log.info("Updating image order: fileHistoryId={}, order={}", fileHistoryId, imageOrder);

        FileHistory fh = fileHistoryRepository.findById(fileHistoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        List<PdfImage> images = pdfImageRepository.findByFileHistoryOrderByPageNumber(fh);
        int n = images.size();

        List<Integer> perm = parseZeroBasedPermutation(imageOrder, n);
        if (perm == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT); // 또는 기본 순서로 대체하고 저장해도 됨
        }

        String normalized = perm.stream().map(String::valueOf).collect(Collectors.joining(","));
        fh.setImgOrder(normalized);
        fileHistoryRepository.save(fh);

        // 응답: 해당 순서대로 fresh presigned
        List<PdfImage> ordered = perm.stream().map(images::get).collect(Collectors.toList());
        return ordered.stream()
                .map(pi -> s3Service.generatePresignedUrl(pi.getS3Key(), Duration.ofHours(1)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<String> removePage(Long fileHistoryId, int pageIndex) {
        log.info("Removing page: fileHistoryId={}, pageIndex={}", fileHistoryId, pageIndex);

        FileHistory fh = fileHistoryRepository.findById(fileHistoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        List<PdfImage> before = pdfImageRepository.findByFileHistoryOrderByPageNumber(fh);
        int nBefore = before.size();
        if (pageIndex < 0 || pageIndex >= nBefore) throw new BusinessException(ErrorCode.INVALID_INPUT);

        PdfImage target = before.get(pageIndex);
        try { s3Service.delete(target.getS3Key()); } catch (Exception ignore) {}
        pdfImageRepository.delete(target);

        // 현재 저장된 순서를 0-based 퍼뮤테이션으로 파싱 (없거나 불량이면 기본순서)
        List<Integer> permBefore = parseZeroBasedPermutation(fh.getImgOrder(), nBefore);
        if (permBefore == null) {
            permBefore = new ArrayList<>();
            for (int i = 0; i < nBefore; i++) permBefore.add(i);
        }

        // 삭제된 인덱스 제거 + 큰 값은 -1 재매핑
        List<Integer> permAfter = new ArrayList<>(nBefore - 1);
        for (int v : permBefore) {
            if (v == pageIndex) continue;
            permAfter.add(v > pageIndex ? v - 1 : v);
        }

        int nAfter = nBefore - 1;
        if (permAfter.size() != nAfter) {
            // 이론상 같아야 함(퍼뮤테이션이었고 하나 제거했으므로)
            throw new BusinessException(ErrorCode.INVALID_BUSINESS_LOGIC);
        }

        String normalizedAfter = permAfter.stream().map(String::valueOf).collect(Collectors.joining(","));
        fh.setImgOrder(normalizedAfter);
        fh.setImgCount(nAfter);
        fileHistoryRepository.save(fh);

        // 삭제 후 목록 재조회 + 새로운 퍼뮤테이션 적용
        List<PdfImage> remaining = pdfImageRepository.findByFileHistoryOrderByPageNumber(fh);
        List<PdfImage> ordered = permAfter.stream().map(remaining::get).collect(Collectors.toList());

        return ordered.stream()
                .map(pi -> s3Service.generatePresignedUrl(pi.getS3Key(), Duration.ofHours(1)))
                .collect(Collectors.toList());
    }

}
