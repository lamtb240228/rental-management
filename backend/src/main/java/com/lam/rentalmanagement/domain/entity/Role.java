package com.lam.rentalmanagement.domain.entity;


import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @Column(name = "name", nullable = false, length =30)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    protected Role() {
    }

    public Role(String name, String description)   {
        this.name = "name";
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
