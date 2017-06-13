package q2p.hardexporter;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public final class HardExporter {
	public static void main(final String[] args) {
		final File readFile = pickFile();
		if(readFile == null) return;

		final String searchFor = getPattern();
		if(searchFor == null) return;

		final Integer befPad = getPadding(false);
		if(befPad == null) return;

		final Integer aftPad = getPadding(true);
		if(aftPad == null) return;

		final File writeFile = writeFile();
		if(writeFile == null) return;

		final String err = hardWork(readFile, writeFile, searchFor, befPad, aftPad);

		if(err == null)
			JOptionPane.showMessageDialog(null, "Process finished.", "Done", JOptionPane.INFORMATION_MESSAGE);
		else
			JOptionPane.showMessageDialog(null, err, "Error", JOptionPane.ERROR_MESSAGE);
	}

	private static String hardWork(final File inFile, final File outFile, final String pattern, final int befPad, final int aftPad) {
		byte[] bytePattern = pattern.getBytes(StandardCharsets.UTF_8);

		long diskPointer = 0;

		int buffLength = 0;

		final byte[] buff = new byte[4*1024];

		int samePatternPointer = 0;

		int buffPointer = 0;

		long foundPointer = -1;

		try(FileOutputStream fos = new FileOutputStream(outFile, true); RandomAccessFile raf = new RandomAccessFile(inFile, "r")) {
			final long fileLength = raf.length();

			do {
				if(buffPointer == buffLength) {
					diskPointer += buffLength;

					if(diskPointer == fileLength)
						break;

					buffLength = raf.read(buff);

					buffPointer = 0;
				}

				if(buff[buffPointer] == bytePattern[samePatternPointer]) {
					if(samePatternPointer == 0) {
						foundPointer = diskPointer + buffPointer;
					}

					samePatternPointer++;

					if(samePatternPointer == bytePattern.length) {
						long spos = Math.max(foundPointer - befPad, 0);
						long epos = Math.min(foundPointer + bytePattern.length + aftPad, fileLength);

						raf.seek(spos);

						for(long i = spos; i != epos; i += buffLength) {
							buffLength = raf.read(buff, 0, (int)(epos - i));

							fos.write(buff, 0, buffLength);
						}
						fos.write('\n');

						diskPointer = foundPointer+bytePattern.length;

						raf.seek(diskPointer);

						buffLength = raf.read(buff);

						buffPointer = 0;

						samePatternPointer = 0;

						continue;
					}
				} else {
					samePatternPointer = 0;
					foundPointer = -1;
				}

				buffPointer++;
			} while(true);

			fos.flush();
		} catch(final Exception e) {
			return e.getMessage();
		}
		return null;
	}

	private static String getPattern() {
		final String ret = JOptionPane.showInputDialog(null, "Type a pattern to look for.", "Input Pattern", JOptionPane.QUESTION_MESSAGE);
		return (ret == null || ret.length() == 0) ? null : ret;
	}

	private static Integer getPadding(final boolean after) {
		final String ret = JOptionPane.showInputDialog(null, "Type how many symbols to reserve "+(after?"after":"before")+" pattern.", (after?"After":"Before")+" Padding", JOptionPane.QUESTION_MESSAGE);
		if(ret == null)
			return null;

		try {
			final int reti = Integer.parseInt(ret);
			return reti < 0 ? null : reti;
		} catch(final NumberFormatException e) {
			return null;
		}
	}

	private static File pickFile() {
		JFileChooser fc = new JFileChooser();
		fc.setApproveButtonText("Search here");
		fc.setDialogType(JFileChooser.OPEN_DIALOG);
		fc.setDialogTitle("Search where?");
		fc.setFileHidingEnabled(true);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setMultiSelectionEnabled(false);
		if(fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return null;

		return fc.getSelectedFile();
	}

	private static File writeFile() {
		JFileChooser fc = new JFileChooser();
		fc.setApproveButtonText("Write here");
		fc.setDialogType(JFileChooser.OPEN_DIALOG);
		fc.setDialogTitle("Write where?");
		fc.setFileHidingEnabled(true);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setMultiSelectionEnabled(false);
		if(fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
			return null;

		return fc.getSelectedFile();
	}
}
