package de.set.trainingUI.generators;

import java.io.File;
import java.util.Random;

@SuppressWarnings("nls")
public class Generators {

    public static void main(final String[] args) throws Exception {
        final File generators = new File(args[0]);
        final File output = new File("taskdb");

        final Random rand = new Random(123);
        for (final File generator : generators.listFiles()) {
            System.out.println("Generating " + generator);
            if (new File(generator, "simple.type").exists()) {
                final SimpleGenerator t = new SimpleGenerator(generator);
                t.generateMultiple(output, rand);
            }
            if (new File(generator, "mutation.type").exists()) {
                final MutationGenerator t = new MutationGenerator(generator);
                t.generateMultiple(output, rand);
            }
        }

    }

}
