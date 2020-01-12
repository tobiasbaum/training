package de.set.trainingUI.generators;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

public class SimpleGenerator extends Generator {
    private final File directory;
    private final Properties values;

    public SimpleGenerator(final File template) throws IOException {
        this.directory = template;
        this.values = new Properties();
        try (FileInputStream s = new FileInputStream(new File(template, "template.properties"))) {
            this.values.load(s);
        }
    }

    @Override
    public Properties getValues() {
        return this.values;
    }

    @Override
    public void generateMultiple(final File targetDir, final Random rand) throws NoSuchAlgorithmException, IOException {
        final int count = Integer.parseInt(this.values.getProperty("count"));
        for (int i = 0; i < count; i++) {
            this.generate(targetDir, rand);
        }
    }

    @Override
    public void generate(final File targetDir, final Random rand) throws IOException, NoSuchAlgorithmException {
        final VelocityContext params = new VelocityContext();
        for (final Entry<Object, Object> e : this.values.entrySet()) {
            final String valueSet = e.getValue().toString();
            final List<String> values = Arrays.asList(valueSet.split("\\|"));
            params.put(e.getKey().toString(), values.get(rand.nextInt(values.size())));
        }

        final String s = this.generateFile("source", params);
        final String id = this.hash(s);

        final File dir = new File(targetDir, id);
        dir.mkdir();
        Files.write(dir.toPath().resolve("source"), s.getBytes("UTF-8"));

        final String p = this.generateFile("task.properties", params);
        Files.write(dir.toPath().resolve("task.properties"), p.getBytes("UTF-8"));
    }

    private String generateFile(final String string, final VelocityContext params) {
        final org.apache.velocity.Template t = Velocity.getTemplate(new File(this.directory, string).getPath());

        final StringWriter w = new StringWriter();
        t.merge(params, w);
        return w.toString();
    }

    private String hash(final String s) throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final byte[] encodedhash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(encodedhash);
    }

    private static String bytesToHex(final byte[] hash) {
        final StringBuffer hexString = new StringBuffer();
        for (final byte element : hash) {
            final String hex = Integer.toHexString(0xff & element);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static void main(final String[] args) throws Exception {
        final File template = new File(args[0]);

        final SimpleGenerator t = new SimpleGenerator(template);
        t.generateMultiple(new File("taskdb"), new Random(42));
    }

}
