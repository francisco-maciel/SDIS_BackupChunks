package server.messages;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;

import server.Version;

public abstract class Message {

	protected MessageType type;
	protected String version;
	protected String fileId;
	protected int chunkNo;

	public Message(String fileId, int chunkNo) {
		this(Version.get(), fileId, chunkNo);
	}

	public Message(String version, String fileId, int chunkNo) {
		this.fileId = fileId;
		this.version = version;
		this.type = null;
		this.chunkNo = chunkNo;
	}

	public MessageType getType() {
		return type;
	}

	public String getVersion() {
		return version;
	}

	public String getFileId() {
		return fileId;
	}

	public int getChunkNo() {
		return chunkNo;
	}

	public void setVersion() {
		this.version = Version.get();
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public void setChunkNo(int chunkNo) {
		this.chunkNo = chunkNo;
	}

	public abstract String toMessage();

	public static Message parse(String message) throws UnrecognizedMessageException {
		Message parsed = null;

		if (message == null)
			throw new UnrecognizedMessageException();
		if ((parsed = parseHead(message)) == null)
			throw new UnrecognizedMessageException();

		return parsed;

	}

	private static Message parseHead(String message) throws UnrecognizedMessageException {
		Message parsed = null;
		int lineIndex = 0;
		HashMap<MessageType, Integer> wordsByType = new HashMap<MessageType, Integer>();
		fillHash(wordsByType);

		// parse header
		InputStream is = new ByteArrayInputStream(message.getBytes());
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		String line = "";
		try {
			while ((line = br.readLine()) != null) {
				if (line.length() == 0)
					break; // empty line end header

				// Traditional recognized line
				if (lineIndex == 0) {

					String[] words = line.split(" ");
					if (words.length < 2)
						return null;

					MessageType type = getMessageType(words[0]);
					if (words.length != wordsByType.get(type))
						return null;

					parsed = generateMessageHeader(type, words);

				}
				// else ignored

				lineIndex++;
			}
			if (line == null)
				return null;
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (lineIndex < 1)
			return null; // less than 1 line, not even header
		else {
			if (parsed.type.equals(MessageType.PUTCHUNK)
					|| parsed.type.equals(MessageType.CHUNK)) {

				try {
					int size = 0;

					char[] buf = new char[1000 * 64 + 1];
					int read = 0;
					int newchars = 0;
					boolean hasBody = false;
					while ((newchars = br.read(buf)) != -1) {
						hasBody = true;
						read += newchars;
					}
					String fullBody = new String(Arrays.copyOf(buf, read));

					parsed.setBody(fullBody.getBytes());

				} catch (IOException e) {
					e.printStackTrace();
				}

			}

			return parsed;
		}
	}

	public abstract void setBody(byte[] bytes);

	private static Message generateMessageHeader(MessageType type, String[] words) throws UnrecognizedMessageException {
		Message newMessage = null;
		try {
			if (type.equals(MessageType.PUTCHUNK))
				newMessage = new MessagePutChunk(words[1], words[2],
						Integer.parseInt(words[3]), null,
						Integer.parseInt(words[4]));
			else if (type.equals(MessageType.CHUNK))
				newMessage = new MessageChunk(words[1], words[2],
						Integer.parseInt(words[3]), null);
			else if (type.equals(MessageType.STORED))
				newMessage = new MessageStored(words[1], words[2],
						Integer.parseInt(words[3]));
			else if (type.equals(MessageType.GETCHUNK))
				newMessage = new MessageGetChunk(words[1], words[2],
						Integer.parseInt(words[3]));
			else if (type.equals(MessageType.DELETE))
				newMessage = new MessageDelete(words[1]);
			else if (type.equals(MessageType.REMOVED))
				newMessage = new MessageRemoved(words[1],
						Integer.parseInt(words[2]));
		} catch (NumberFormatException e) {
			throw new UnrecognizedMessageException();
		}
		return newMessage;
	}

	private static void fillHash(HashMap<MessageType, Integer> wordsByType) {
		wordsByType.put(MessageType.PUTCHUNK, 5);
		wordsByType.put(MessageType.CHUNK, 4);
		wordsByType.put(MessageType.GETCHUNK, 4);
		wordsByType.put(MessageType.REMOVED, 3);
		wordsByType.put(MessageType.DELETE, 2);
		wordsByType.put(MessageType.STORED, 4);
	}

	private static MessageType getMessageType(String header) throws UnrecognizedMessageException {
		if (header.equalsIgnoreCase("PUTCHUNK"))
			return MessageType.PUTCHUNK;
		else if (header.equalsIgnoreCase("CHUNK"))
			return MessageType.CHUNK;
		else if (header.equalsIgnoreCase("DELETE"))
			return MessageType.DELETE;
		else if (header.equalsIgnoreCase("GETCHUNK"))
			return MessageType.GETCHUNK;
		else if (header.equalsIgnoreCase("REMOVED"))
			return MessageType.REMOVED;
		else if (header.equalsIgnoreCase("STORED"))
			return MessageType.STORED;

		throw new UnrecognizedMessageException();
	}
}