package it.uniroma2.sel.ebpmn.logger;

//import java.time.LocalDateTime;

/**
 * Value object holding a single process-log entry (XES / event-log format).
 *
 * <p>One {@code LogData} instance is created per activity execution and written
 * to the {@link it.uniroma2.sel.ebpmn.logger.CSVLogger} by each flow node.
 * Fields follow the XES standard where applicable ({@code caseId} → case id,
 * {@code activity} → concept:name, {@code startTimestamp} /
 * {@code completeTimestamp} → lifecycle timestamps).
 *
 * <p>Resource lists and roles are colon-separated strings
 * (e.g., {@code "res1:res2"}) to accommodate multi-resource tasks.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see it.uniroma2.sel.ebpmn.logger.Logger
 * @see it.uniroma2.sel.ebpmn.logger.CSVLogger
 * @see CommunicationKind
 */
public class LogData {
	/** Case identifier (maps to the token ID). */
	private String caseId;
	/** ISO-style start timestamp of the activity (empty string if not applicable). */
	private String startTimestamp;
	/** ISO-style completion timestamp of the activity. */
	private String completeTimestamp;
	/** Activity name (the flow-node name). */
	private String activity;
	/** Colon-separated list of resource names involved in the activity. */
	private String resources;
	/** Colon-separated list of resource roles corresponding to {@link #resources}. */
	private String roles;
	/** Name of the local participant (pool) executing the activity. */
	private String localEntity;
	/** Name of the remote participant involved in communication (may be empty). */
	private String remoteEntity;
	/** Type of inter-participant communication for this log entry. */
	private CommunicationKind commType;
	/** Application-level message payload (for message-flow activities). */
	private String data;
	
	public LogData() {};
	
	public LogData(String caseId, String startTimestamp, String completeTimestamp, String activity,
			String resources, String roles, String localEntity, String remoteEntity, CommunicationKind commType,
			String iP, String data) {
		this.caseId = caseId;
		this.startTimestamp = startTimestamp;
		this.completeTimestamp = completeTimestamp;
		this.activity = activity;
		this.resources = resources;
		this.roles = roles;
		this.localEntity = localEntity;
		this.remoteEntity = remoteEntity;
		this.commType = commType;
		this.data = data;
	}

	public String getCaseId() {
		return caseId;
	}

	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}

	public String getStartTimestamp() {
		return startTimestamp;
	}

	public void setStartTimestamp(String string) {
		this.startTimestamp = string;
	}

	public String getCompleteTimestamp() {
		return completeTimestamp;
	}

	public void setCompleteTimestamp(String string) {
		this.completeTimestamp = string;
	}

	public String getActivity() {
		return activity;
	}

	public void setActivity(String activity) {
		this.activity = activity;
	}

	public String getResources() {
		return resources;
	}

	public void setResources(String resources) {
		this.resources = resources;
	}

	public String getRoles() {
		return roles;
	}

	public void setRoles(String roles) {
		this.roles = roles;
	}

	public String getLocalEntity() {
		return localEntity;
	}

	public void setLocalEntity(String localEntity) {
		this.localEntity = localEntity;
	}

	public String getRemoteEntity() {
		return remoteEntity;
	}

	public void setRemoteEntity(String remoteEntity) {
		this.remoteEntity = remoteEntity;
	}

	public CommunicationKind getCommType() {
		return commType;
	}

	public void setCommType(CommunicationKind commType) {
		this.commType = commType;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
	
	
	
	
}
