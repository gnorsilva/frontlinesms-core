/**
 * 
 */
package net.frontlinesms.mmsdevice;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.FrontlineMultimediaMessage;
import net.frontlinesms.data.domain.FrontlineMultimediaMessagePart;
import net.frontlinesms.data.domain.FrontlineMessage.Status;
import net.frontlinesms.data.domain.FrontlineMessage.Type;
import net.frontlinesms.mms.BinaryMmsMessagePart;
import net.frontlinesms.mms.MmsMessage;
import net.frontlinesms.mms.MmsMessagePart;
import net.frontlinesms.mms.TextMmsMessagePart;
import net.frontlinesms.resources.ResourceUtils;

/**
 * @author Alex Anderson <alex@frontlinesms.com>
 */
public class MmsDeviceUtils {
	private static final Logger log = FrontlineUtils.getLogger(MmsDeviceUtils.class);
	
	/** Create a new {@link FrontlineMultimediaMessage} from a {@link MmsMessage} */
	static FrontlineMultimediaMessage create(MmsMessage mms) {
		StringBuilder textContent = new StringBuilder();
		List<FrontlineMultimediaMessagePart> multimediaParts = new ArrayList<FrontlineMultimediaMessagePart>();
		for(MmsMessagePart part : mms.getParts()) {
			if(textContent.length() > 0) textContent.append("; ");
			
			String text;
			FrontlineMultimediaMessagePart mmPart;
			if(part instanceof TextMmsMessagePart) {
				TextMmsMessagePart textPart = (TextMmsMessagePart) part;
				text = textPart.toString();
				mmPart = FrontlineMultimediaMessagePart.createTextPart(textPart.getContent());
			} else if(part instanceof BinaryMmsMessagePart) {
				BinaryMmsMessagePart imagePart = (BinaryMmsMessagePart) part;
				text = "Image: " + imagePart.getFilename();
				mmPart = createBinaryPart(imagePart);
			} else {
				text = "Unhandled: " + part.toString();
				mmPart = FrontlineMultimediaMessagePart.createTextPart("Unhandled: TODO handle this!");
			}
			textContent.append(text);
			multimediaParts.add(mmPart);
		}
		
		FrontlineMultimediaMessage message = new FrontlineMultimediaMessage(Type.RECEIVED, textContent.toString(), multimediaParts);
		message.setRecipientMsisdn("set me please"); // FIXME get recipient address from mms
		message.setSenderMsisdn(mms.getSender());
		message.setStatus(Status.RECEIVED);
		return message;
	}

	private static FrontlineMultimediaMessagePart createBinaryPart(BinaryMmsMessagePart imagePart) {
		// save the binary data to file
		FrontlineMultimediaMessagePart fmmPart = FrontlineMultimediaMessagePart.createBinaryPart(imagePart.getFilename()/*, getThumbnail(imagePart)*/);

		File localFile = getFile(fmmPart);
		while(localFile.exists()) {
			// need to handle file collisions here - e.g. rename the file
			fmmPart.setFilename(getAlternateFilename(fmmPart.getFilename()));
			localFile = getFile(fmmPart);
		}
		writeFile(localFile, imagePart.getData());
		return fmmPart; 
	}
	
	private static String getAlternateFilename(String filename) {
		String namePart = FrontlineUtils.getFilenameWithoutAnyExtension(filename);
		String extension = FrontlineUtils.getWholeFileExtension(filename);
		return namePart + '_' + new Random().nextInt(99) + '.' + extension;
	}

	private static void writeFile(File file, byte[] data) {
		FileOutputStream fos = null;
		BufferedOutputStream out = null;
		try {
			file.getParentFile().mkdirs();
			fos = new FileOutputStream(file);
			out = new BufferedOutputStream(fos);
			out.write(data);
		} catch (IOException ex) {
			log.warn("Failed to write MMS file: " + file.getAbsolutePath(), ex);
		} finally {
			if(out != null) try { out.close(); } catch(IOException ex) { /* ah well :/ */ }
			if(fos != null) try { fos.close(); } catch(IOException ex) { /* ah well :/ */ }
		}
	}

	public static File getFile(FrontlineMultimediaMessagePart part) {
		return new File(new File(ResourceUtils.getConfigDirectoryPath(), "data/mms"), part.getFilename());
	}
}
