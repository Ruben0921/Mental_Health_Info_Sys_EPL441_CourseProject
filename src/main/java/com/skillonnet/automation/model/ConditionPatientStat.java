package com.skillonnet.automation.model;

/**
 * Aggregate: number of distinct patients per condition in a date range.
 * Used for weekly management reporting (Management viewpoint req. 2).
 */
public class ConditionPatientStat {

    private int    conditionId;
    private String conditionName;
    private long   patientCount;

    public ConditionPatientStat() {}

    public int    getConditionId()              { return conditionId; }
    public String getConditionName()            { return conditionName; }
    public long   getPatientCount()             { return patientCount; }

    public void setConditionId(int conditionId)          { this.conditionId = conditionId; }
    public void setConditionName(String conditionName)   { this.conditionName = conditionName; }
    public void setPatientCount(long patientCount)       { this.patientCount = patientCount; }
}
