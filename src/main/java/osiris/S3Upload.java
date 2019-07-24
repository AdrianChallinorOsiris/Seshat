package osiris;

import com.amazonaws.services.s3.transfer.Upload;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class S3Upload {
	private Upload upload;
	private String containerFile;
	private long size;
}
