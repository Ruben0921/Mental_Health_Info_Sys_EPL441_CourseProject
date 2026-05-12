package com.skillonnet.automation.model;

/** Diagnosis/condition vocabulary item (domain model; may not be fully persisted in DDL). */
public class Condition {

    private int conditionId;
    private String name;

    public int getConditionId() {
        return conditionId;
    }

    public void setConditionId(int conditionId) {
        this.conditionId = conditionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
