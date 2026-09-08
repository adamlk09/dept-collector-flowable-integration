package com.mercure.recouvrement.workflow.batch;

/** One row of the irregular-portfolio CSV — doc §12's ingestion pipeline. */
public class BankingCaseRow {

    private String debtId;
    private String customerId;
    private String clientSegment;
    private Integer overdueDays;
    private Double totalExposure;
    private String phoneNumber;
    private String address;

    public String getDebtId() {
        return debtId;
    }

    public void setDebtId(String debtId) {
        this.debtId = debtId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getClientSegment() {
        return clientSegment;
    }

    public void setClientSegment(String clientSegment) {
        this.clientSegment = clientSegment;
    }

    public Integer getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(Integer overdueDays) {
        this.overdueDays = overdueDays;
    }

    public Double getTotalExposure() {
        return totalExposure;
    }

    public void setTotalExposure(Double totalExposure) {
        this.totalExposure = totalExposure;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
