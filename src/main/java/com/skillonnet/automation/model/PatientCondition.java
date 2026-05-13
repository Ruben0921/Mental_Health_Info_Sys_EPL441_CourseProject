package com.skillonnet.automation.model;

public class PatientCondition {
    private int patientConditionId;
    private int patientId;
    private int conditionId;
    private String conditionName;
    private int appointmentId;
    private String diagnosisDate;
    private String notes;
 
    public int getPatientConditionId(){ 
		return patientConditionId; 
	}

    public void setPatientConditionId(int v){ 
		this.patientConditionId = v; 
	}

    public int getPatientId(){ 
		return patientId; 
	}

    public void setPatientId(int v){ 
		this.patientId = v; 
	}

	public int getConditionId(){ 
		return conditionId; 
	}

    public void setConditionId(int v){ 
		this.conditionId = v; 
	}

    public String getConditionName(){ 
		return conditionName; 
	}

    public void setConditionName(String v){ 
		this.conditionName = v; 
	}

    public int getAppointmentId(){ 
		return appointmentId; 
	}

    public void setAppointmentId(int v){ 
		this.appointmentId = v;
	 }

    public String getDiagnosisDate(){ 
		return diagnosisDate; 
	}

    public void setDiagnosisDate(String v){ 
		this.diagnosisDate = v; 
	}

    public String getNotes(){ 
		return notes; 
	}

    public void setNotes(String v){
		 this.notes = v; 
	}
}