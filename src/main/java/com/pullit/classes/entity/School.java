package com.pullit.classes.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "schools")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class School {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_name", nullable = false)
    private String schoolName;

    @Column(name = "address_jibun")
    private String addressJibun;  // 지번주소
    
    @Column(name = "address_road")
    private String addressRoad;  // 도로명주소
    
    @Column(name = "sido_office")
    private String sidoOffice;  // 시도교육청명
    
    @Column(name = "edu_office")
    private String eduOffice;  // 교육지원청명
    


}
