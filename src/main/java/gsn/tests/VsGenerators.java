package gsn.tests;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class VsGenerators {

	public static void main(String[] args) throws IOException {
		File vs = new File("virtual-sensors/memoryDataVS.xml");
		for (int i=0;i<1000;i++){
			Path path = vs.toPath();
			Charset charset = StandardCharsets.UTF_8;
			
			String content = new String(Files.readAllBytes(path), charset);
			content = content.replaceAll("MemoryMonitorVS", "Mem"+i);
			Files.write(new File("virtual-sensors/mm"+i+".xml").toPath(), content.getBytes(charset));
		}
			
	}

}
