package de.set.assessmentUi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Experiments {

    private static final Experiments INSTANCE = new Experiments();

    private final AtomicLong idCounter = new AtomicLong(System.currentTimeMillis() - 40L * 365 * 24 * 60 * 60 * 1000);
    private final ConcurrentHashMap<Long, Experiment> map = new ConcurrentHashMap<>();

    private Experiments() {
    }

    public static Experiments getInstance() {
        return INSTANCE;
    }

    public Experiment getNew() {
        final Experiment ret = new Experiment(this.idCounter.incrementAndGet());
        this.map.put(ret.getId(), ret);
        DataLog.log(ret.getId(), "alpha id is " + ret.getIdAlpha());
        DataLog.log(ret.getId(), "private key is " + ret.getPrivateKey());
        return ret;
    }

    public Experiment get(long id) {
        return this.map.get(id);
    }

    public List<Experiment> getExperiments() {
        final List<Experiment> ret = new ArrayList<>();
        this.map.forEachValue(1, (Experiment x) -> ret.add(x));
        Collections.sort(ret, (Experiment e1, Experiment e2) -> Long.compare(e1.getId(), e2.getId()));
        return ret;
    }

    public void cancel(long id, Treatments t) {
        final Experiment e = this.map.get(id);
        if (e == null) {
            DataLog.log(id, "cancel not possible, experiment does not exist");
        } else {
            e.cancel(t);
            DataLog.log(id, "experiment cancelled");
        }
    }

}
