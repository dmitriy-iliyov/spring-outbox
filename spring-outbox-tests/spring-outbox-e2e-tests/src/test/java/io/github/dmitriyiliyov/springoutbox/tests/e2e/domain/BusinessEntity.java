package io.github.dmitriyiliyov.springoutbox.tests.e2e.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "business_events")
public class BusinessEntity {

    @Id
    private UUID verifyId;

    public BusinessEntity() {}

    public BusinessEntity(UUID verifyId) {
        this.verifyId = verifyId;
    }

    public UUID getVerifyId() {
        return verifyId;
    }

    public void setVerifyId(UUID verifyId) {
        this.verifyId = verifyId;
    }
}
