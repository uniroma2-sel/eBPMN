package it.uniroma2.sel.ebpmn.logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * CSV-format implementation of {@link Logger} for the eBPMN simulation framework.
 *
 * <p>Each call to {@link #write(LogData)} appends one comma-separated row to the
 * output file. The file is created (or overwritten) when this logger is constructed,
 * and the following header row is written immediately:</p>
 *
 * <pre>
 * CaseId,StartTimestamp,CompletionTimestamp,Activity,Resource,Role,
 * LocalEntity,RemoteEntity,CommunicationType,Data
 * </pre>
 *
 * <p>Column semantics:</p>
 * <ul>
 *   <li><b>CaseId</b> – identifier of the token / process instance being logged</li>
 *   <li><b>StartTimestamp</b> – simulation time at which the activity started</li>
 *   <li><b>CompletionTimestamp</b> – simulation time at which the activity completed</li>
 *   <li><b>Activity</b> – name of the BPMN node (task, event, gateway, …) being executed</li>
 *   <li><b>Resource</b> – colon-separated list of resource names involved</li>
 *   <li><b>Role</b> – colon-separated list of roles associated with the resources</li>
 *   <li><b>LocalEntity</b> – name of the local {@link it.uniroma2.sel.ebpmn.bpmn.Participant} producing this entry</li>
 *   <li><b>RemoteEntity</b> – name of the remote participant, if any message exchange occurred</li>
 *   <li><b>CommunicationType</b> – kind of communication ({@link CommunicationKind})</li>
 *   <li><b>Data</b> – optional payload or annotation carried by the message or token</li>
 * </ul>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see Logger
 * @see LogData
 * @see CommunicationKind
 */
public class CSVLogger extends Logger {

	/**
	 * Creates a new {@code CSVLogger} that writes to the specified file.
	 *
	 * <p>The file is opened (or created) immediately and the CSV header row is
	 * written. If the file cannot be created or opened, the stack trace is
	 * printed to standard error.</p>
	 *
	 * @param fileName path (relative or absolute) of the CSV output file to create or overwrite
	 */
	public CSVLogger(String fileName) {
		super(fileName);
		try {
			writer = Files.newBufferedWriter(Paths.get(fileName));
			//adding the csv header
			writer.write("CaseId,"
					+ "StartTimestamp,"
					+ "CompletionTimestamp,"
					+ "Activity,"
					+ "Resource,"
					+ "Role,"
					+ "LocalEntity,"
					+ "RemoteEntity,"
					+ "CommunicationType,"
					+ "Data");
			writer.newLine();


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Appends a single comma-separated row to the CSV output file.
	 *
	 * <p>The row is formatted as:
	 * {@code CaseId,StartTimestamp,CompletionTimestamp,Activity,Resource,Role,
	 * LocalEntity,RemoteEntity,CommunicationType,Data}
	 * followed by a platform-specific line separator.</p>
	 *
	 * @param data the log entry whose fields are serialised into the CSV row;
	 *             must not be {@code null}
	 */
	//@Override
	public void write(LogData data) {
		try {
			//adding comma separated values
			writer.write(String.valueOf(data.getCaseId()) + ","
					+ data.getStartTimestamp() + ","
					+ data.getCompleteTimestamp() + ","
					+ data.getActivity() + ","
					+ data.getResources() + ","
					+ data.getRoles() + ","
					+ data.getLocalEntity() + ","
					+ data.getRemoteEntity() + ","
					+ data.getCommType() + ","
					+ data.getData());
			writer.newLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
