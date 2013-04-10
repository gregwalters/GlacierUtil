import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.model.ListVaultsRequest;
import com.amazonaws.services.glacier.model.ListVaultsResult;
import com.amazonaws.services.glacier.model.DeleteVaultRequest;
import com.amazonaws.services.glacier.model.InitiateJobRequest;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.UploadResult;
import com.amazonaws.services.glacier.model.JobParameters;
import com.amazonaws.services.glacier.model.InitiateJobResult;
import com.amazonaws.services.glacier.model.GetJobOutputResult;
import com.amazonaws.services.glacier.model.GetJobOutputRequest;
import com.amazonaws.services.glacier.model.ListJobsRequest;
import com.amazonaws.services.glacier.model.ListJobsResult;
import com.amazonaws.services.glacier.model.DescribeJobRequest;
import com.amazonaws.services.glacier.model.DescribeJobResult;
import com.amazonaws.services.glacier.model.DeleteArchiveRequest;
import com.amazonaws.services.glacier.model.CreateVaultResult;
import com.amazonaws.services.glacier.model.CreateVaultRequest;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ArrayIndexOutOfBoundsException;
import java.util.Date;
import java.util.Properties;

public class GlacierUtil {

	BasicAWSCredentials creds;
	AmazonGlacierClient client;
	boolean isSetup = false;
	String accessKey;
	String secretKey;
	String endPoint;

	public static void main(String... args) {
		GlacierUtil util = new GlacierUtil();
		try {
			switch (Integer.parseInt(args[0])) {
				case 1:		util.upload(args[1], args[2]);
						break;
				case 2:		util.listVaults();
						break;
				case 3:		util.deleteVault(args[1]);
						break;
				case 4:		util.listVaultContents(args[1]);
						break;
				case 5:		util.getJobStatus(args[1], args[2]);
						break;
				case 6:		util.listJobs(args[1]);
						break;
				case 7:		util.getJobOutput(args[1], args[2]);
						break;
				case 8:		util.deleteArchive(args[1], args[2]);
						break;
				case 9:		util.createVault(args[1]);
						break;
				default:	util.printHelp();
						break;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			util.printHelp();
		}
	}

	public void setup() {
		String propFilePath = System.getProperty("glacier.file", "glacier.properties");
		File propFile = new File(propFilePath);
		Properties properties = new Properties();
		try {
			FileInputStream fis = new FileInputStream(propFile);
			properties.load(fis);
		} catch (Exception e) {
			System.err.println("Properties file does not exist. Trying global system properties");
		}
		accessKey = properties.getProperty("access.key");
		secretKey = properties.getProperty("secret.key");
		endPoint = properties.getProperty("endpoint");
		if ( accessKey == null || secretKey == null || endPoint == null ) {
			System.err.println("You must set your access key, secret key and endpoint in the glacier.properties file.");
			System.exit(1);
		}
		creds = new BasicAWSCredentials(accessKey,secretKey);
		client = new AmazonGlacierClient(creds);
		client.setEndpoint(endPoint);
		isSetup = true;
	}

	public void isSetup() {
		if (!isSetup) {
			setup();
		}
	}

	public void createVault(String vault) {
		isSetup();
		CreateVaultRequest cvr = new CreateVaultRequest(vault);
		CreateVaultResult vaultResult = client.createVault(cvr);
		System.out.println(vaultResult);
	}

	public void deleteArchive(String vault, String archiveId) {
		isSetup();
		DeleteArchiveRequest dar = new DeleteArchiveRequest(vault, archiveId);
		client.deleteArchive(dar);
	}

	public void getJobOutput(String vault, String jobId) {
		isSetup();
		GetJobOutputRequest outputRequest = new GetJobOutputRequest()
			.withVaultName(vault)
			.withJobId(jobId);
		GetJobOutputResult outputResult = client.getJobOutput(outputRequest);
		InputStream is = outputResult.getBody();
		int status = 0;
		while ( status != -1) {
			try {
				byte[] bytes = new byte[is.available()];
				status = is.read(bytes);
				System.out.println(new String(bytes));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void listJobs(String vault) {
		isSetup();
		ListJobsRequest lr = new ListJobsRequest(vault);
		ListJobsResult listResult = client.listJobs(lr);
		System.out.println(listResult);
	}

	public void getJobStatus(String vault, String jobId) {
		isSetup();
		DescribeJobRequest jr = new DescribeJobRequest(vault, jobId);
		DescribeJobResult jobResult = client.describeJob(jr);
		System.out.println(jobResult.getStatusCode());
	}

	public void listVaults() {
		isSetup();
		ListVaultsResult vaults = client.listVaults(new ListVaultsRequest());
		System.out.println(vaults);
	}

	public void deleteVault(String vault) {
		isSetup();
		DeleteVaultRequest dvr = new DeleteVaultRequest(vault);
		client.deleteVault(dvr);
	}

	public void listVaultContents(String vault) {
		isSetup();
		InitiateJobRequest jr = new InitiateJobRequest().withVaultName(vault)
			.withJobParameters(
				new JobParameters().withType("inventory-retrieval")
			);
		InitiateJobResult jobResult = client.initiateJob(jr);
		String jobId = jobResult.getJobId();
		System.out.println("Job id: " + jobId);
	}

	public void upload(String vault, String uploadSize) {
		isSetup();
		int maxRead = Integer.parseInt(uploadSize)*1024*1024;
		byte[] bytes = new byte[maxRead];
		File file = new File("/tmp/zfs_backup");
		ArchiveTransferManager atm = new ArchiveTransferManager(client, creds);
		try {
			int status = 0;
			while (status != -1) {
				FileOutputStream fos = new FileOutputStream(file);
				//CipherOutputStream cos = new CipherOutputStream(fos, c);
				status = System.in.read(bytes);
				if (status == -1) {
					break;
				}
				if (status < maxRead) {
					byte[] shorty = new byte[status];
					for (int i = 0 ; i < status; i++) {
						shorty[i] = bytes[i];
					}
					fos.write(shorty);
				} else {
					fos.write(bytes);
				}
				long maxBytes = Runtime.getRuntime().maxMemory();
				long currentBytes = Runtime.getRuntime().totalMemory();
				long freeBytes = Runtime.getRuntime().freeMemory();
				UploadResult result = atm.upload(vault, "archive " + (new Date()), file);
				System.out.println("Archive ID: " + result.getArchiveId());
				System.err.println("Heap Usage:");
				System.err.println("Max: " + maxBytes + " Current: " + currentBytes + " Free: " + freeBytes);
				System.err.println("Bytes read from stream: " + status + " Bytes uploaded: " + file.length());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			file.delete();
		}
	}

	public void printHelp() {
		System.out.println("1: upload <vault> <max upload size>");
		System.out.println("2: listVaults");
		System.out.println("3: delete <vault>");
		System.out.println("4: start job to list vault contents <vault>");
		System.out.println("5: get job status <vault> <jobId>");
		System.out.println("6: list jobs <vault>");
		System.out.println("7: get job output <vault> <jobId>");
		System.out.println("8: delete archive <vault> <archiveId>");
		System.out.println("9: create <vault>");
		System.exit(0);
	}
}
