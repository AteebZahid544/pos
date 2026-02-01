package com.example.pos.entity.pos;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "client_requirements")
public class ClientRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "requirements",length = 2000)
    private String requirements;

    @Column(name = "is_active")
    private Boolean active;


}
