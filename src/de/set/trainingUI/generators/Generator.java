package de.set.trainingUI.generators;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.Random;

public abstract class Generator {

    public void generateMultiple(final File targetDir, final Random rand) throws NoSuchAlgorithmException, IOException {
        final int count = Integer.parseInt(this.getValues().getProperty("count"));
        for (int i = 0; i < count; i++) {
            this.generate(targetDir, rand);
        }
    }

    protected abstract Properties getValues();

    protected abstract void generate(File targetDir, Random rand) throws IOException, NoSuchAlgorithmException;

}
