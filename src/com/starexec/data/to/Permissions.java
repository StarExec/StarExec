package com.starexec.data.to;

public class Permissions {
	private boolean isAdmin;
	private boolean canSubmit;
	private boolean canExecute;
	private boolean canCancel;
	private boolean canCreate;
	private String userID;
	
	public Permissions(String userID, boolean isAdmin, boolean canSubmit, boolean canExecute, boolean canCancel, boolean canCreate){
		this.isAdmin = isAdmin;
		this.canSubmit = canSubmit;
		this.canExecute = canExecute;
		this.canCancel = canCancel;
		this.canCreate = canCreate;	
		this.userID = userID;
	}
	
	public boolean isAdmin() {
		return isAdmin;
	}
	public boolean isCanSubmit() {
		return canSubmit;
	}
	public boolean isCanExecute() {
		return canExecute;
	}
	public boolean isCanCancel() {
		return canCancel;
	}
	public boolean isCanCreate() {
		return canCreate;
	}
	public String getUserID() {
		return userID;
	}		
}
