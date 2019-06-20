package de.set.assessmentUi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.eclipse.jetty.server.handler.AbstractHandler;

import de.set.assessmentUi.DiffData.ChangePart;
import de.set.assessmentUi.Treatments.TreatmentCombination;
import de.set.assessmentUi.Treatments.TreatmentUsageData;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.velocity.VelocityTemplateEngine;

public class ServerMain extends AbstractHandler {

    private static final String BASIC_FILENAME = "[a-zA-Z0-9.]+\\.(html|css|js)";
    private static final Pattern RESOURCE = Pattern.compile("\\/" + BASIC_FILENAME);
    private static final Pattern EXP_PATH = Pattern.compile("\\/exp\\/([0-9]+)\\/" + BASIC_FILENAME);

    private final Treatments treatments = new Treatments(new Random());
    private final String salt;

    private final Map<Long, AssessmentSuite> assessments = new ConcurrentHashMap<>();

    public ServerMain(final String salt) throws IOException {
    	final AssessmentSuite test = new AssessmentSuite(1234, "Herr Baum");
    	test.addStep(new DefectFindTask("/defectFind/defA"));
    	test.addStep(new UnderstandingTask("/understanding/codeA"));
    	test.addStep(new WorkingMemoryTest());
    	this.assessments.put(test.getId(), test);

        this.salt = salt;
        this.initTreatments();
    }

    @Override
    public void handle(final String target,
    		final org.eclipse.jetty.server.Request baseRequest,
                       final HttpServletRequest request,
                       final HttpServletResponse response)
        throws IOException, ServletException {

        baseRequest.setHandled(true);
        final Matcher expPathMatcher = EXP_PATH.matcher(target);
        final Experiment exp;
        if (expPathMatcher.matches()) {
            final long id = Long.parseLong(expPathMatcher.group(1));
            exp = Experiments.getInstance().get(id);
            if (exp == null) {
                this.sendError(404, "experiment does not exist " + id, response);
                return;
            }
        } else {
            exp = null;
        }

        try {
            int index = -1;
            if (exp != null) {
                if (exp.isCancelled()) {
                    this.sendError(500, "Experiment is cancelled", response);
                }

                final Map<String, String[]> params = request.getParameterMap();
                for (final Entry<String, String[]> e : params.entrySet()) {
                    if (exp.isVariableName(e.getKey())) {
                        if (e.getKey().equals("clientSideIpHash") && !e.getValue()[0].equals("unknown")) {
                            exp.setVariable(e.getKey(), new String[] {this.hash(e.getValue()[0])});
                        } else {
                            exp.setVariable(e.getKey(), e.getValue());
                        }
                    } else if (e.getKey().equals("index")
                            && (target.endsWith("codeview.html") || target.contains("/intermediate"))) {
                        index = Integer.parseInt(e.getValue()[0]);
                    } else if (this.isReviewResult(e.getKey()) && target.contains("/intermediate")) {
                        //ok, will be handled later
                    } else if (e.getKey().equals("logContent") && target.contains("/finalquestions")) {
                        //ok, will be handled later
                    } else {
                        this.sendError(500, "Invalid parameter name " + e.getKey(), response);
                        return;
                    }
                }
            }

            if (target.contains("/intermediate") && request.getParameter("logContent") != null) {
                final String log = request.getParameter("logContent");
                for (final String logLine : log.split("\n")) {
                    DataLog.log(exp.getId(), "log from review;" + index + ";" + logLine);
                }
                if (index == 0) {
                    exp.setTestReviewScore(this.countCorrectRemarksInTestReview(log));
                }
            }
            if (target.contains("/finalquestions") && request.getParameter("logContent") != null) {
                for (final String logLine : request.getParameter("logContent").split("\n")) {
                    DataLog.log(exp.getId(), "log from wm test;" + logLine);
                }
            }

            if (target.equals("/admin.html")) {
                final String toCancel = request.getParameter("cancelExperiment");
                if (toCancel != null) {
                    Experiments.getInstance().cancel(Long.parseLong(toCancel), this.treatments);
                }
                this.sendAdminPage(response);
            } else if (target.equals("/start.html")) {
                final Experiment newExperiment = Experiments.getInstance().getNew();
                newExperiment.start();
                final Context ctx = new VelocityContext();
                ctx.put("exp", newExperiment);
                this.interpretTemplate(target + ".vm", ctx, response);
            } else if (target.startsWith("/exp/") && exp != null) {
                DataLog.log(exp.getId(), "request for " + target + " from " + this.hash(request.getRemoteAddr()));
                if (target.endsWith("codeview.html")) {
                    exp.setState("requestReview" + index);
                    this.sendCodeview(exp, index, response);
                } else {
                    final String file = target.substring(target.lastIndexOf('/'));
                    if (file.contains(".html")) {
                        exp.setState("request" + file);
                    }
                    this.sendFileOrTemplate(file, exp, response);
                }
            } else if (RESOURCE.matcher(target).matches()) {
                this.sendFile(target, response);
            } else if (target.equals("/")) {
                this.sendFile("/index.html", response);
            } else {
                this.sendError(404, "Not found " + target, response);
            }
        } catch (final Throwable t) {
            if (exp != null) {
                DataLog.log(exp.getId(), "exception;" + this.traceToString(t));
            }
            throw t;
        }
    }

    private String traceToString(final Throwable t) {
        try {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            sw.close();
            return sw.toString();
        } catch (final IOException e) {
            return "unexpected exception " + e;
        }
    }

    private String hash(final String toHash) {
        try {
            final byte[] bytesOfMessage = (this.salt + toHash).getBytes("UTF-8");
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final byte[] thedigest = md.digest(bytesOfMessage);
            return DatatypeConverter.printHexBinary(thedigest);
        } catch (final UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private int countCorrectRemarksInTestReview(final String log) {
        final String[] lines = log.split("\n");
        final Set<String> foundDefects = new HashSet<>();
        for (final String line : lines) {
            final String[] split = line.trim().split(";");
            if (split.length < 4) {
                continue;
            }
            if (split[1].equals("createReviewRemark")) {
                final String defectKey = this.determineDefectKey(split[2], split[3]);
                if (defectKey != null) {
                    foundDefects.add(defectKey);
                }
            } else if (split[1].equals("deleteReviewRemark")) {
                final String defectKey = this.determineDefectKey(split[2], split[3]);
                if (defectKey != null) {
                    foundDefects.remove(defectKey);
                }
            }
        }
        return foundDefects.size();
    }

    private String determineDefectKey(final String fragment, final String line) {
        if (fragment.equals("#1R") && line.equals("278")) {
            return "offbyone1";
        }
        if (fragment.equals("#1R") && line.equals("280")) {
            return "offbyone2";
        }
        if (fragment.equals("#2R") && line.equals("580")) {
            return "swap";
        }
        return null;
    }

    private boolean isReviewResult(final String key) {
        return key.equals("logContent");
    }

    private void sendCodeview(final Experiment exp, final int index, final HttpServletResponse response) throws IOException {
        final Context ctx = new VelocityContext();
        ctx.put("exp", exp);
        ctx.put("index", index);
        String diffPath;
        Treatment treatment;
        switch (index) {
        case 0:
            diffPath = "warmup";
            treatment = Treatment.WORST_NO_FILES;
            break;
        case 1:
            final TreatmentCombination tc1 = exp.getAssignedTreatment(this.treatments);
            diffPath = tc1.getD1().getPath();
            treatment = tc1.getT1();
            break;
        case 2:
            final TreatmentCombination tc2 = exp.getAssignedTreatment(this.treatments);
            diffPath = tc2.getD2().getPath();
            treatment = tc2.getT2();
            break;
        default:
            throw new AssertionError("invalid index " + index);
        }
        final DiffData diffData = DiffData.load(diffPath, treatment);
        ctx.put("diff", diffData);
        DataLog.log(exp.getId(), "hunkMap;" + treatment + ";" + diffPath + ";"
                        + diffData.getChangeParts().stream().map((final ChangePart cp) -> cp.getIndex() + "=" + cp.getId()).collect(Collectors.toList()));
        this.interpretTemplate("/codeview.html.vm", ctx, response);
    }

    private void sendFileOrTemplate(final String file, final Experiment exp, final HttpServletResponse response) throws IOException {
        if (ServerMain.class.getResource(file + ".vm") != null) {
            final Context ctx = new VelocityContext();
            ctx.put("exp", exp);
            this.interpretTemplate(file + ".vm", ctx, response);
        } else {
            this.sendFile(file, response);
        }
    }

    private void interpretTemplate(final String file, final Context ctx, final HttpServletResponse response) throws IOException {
        this.setMimetype(file, response);
        final InputStream s = ServerMain.class.getResourceAsStream(file);
        if (s == null) {
            this.sendError(404, "Template not found " + file, response);
            return;
        }
        try {
            final OutputStream os = response.getOutputStream();
            try {
                final OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                final boolean success = Velocity.evaluate(
                    ctx,
                    osw,
                    file,
                    new InputStreamReader(s, "UTF-8"));
                osw.flush();
                if (!success) {
                    os.write("no success".getBytes());
                }
            } finally {
                os.close();
            }
        } finally {
            s.close();
        }
    }

    private void sendFile(final String target, final HttpServletResponse response) throws IOException {
        this.setMimetype(target, response);
        final InputStream s = ServerMain.class.getResourceAsStream(target);
        if (s == null) {
            this.sendError(404, "File not found " + target, response);
            return;
        }
        try {
            final OutputStream os = response.getOutputStream();
            try {
                this.copy(s, os);
            } finally {
                os.close();
            }
        } finally {
            s.close();
        }
    }

    private void setMimetype(final String file, final HttpServletResponse response) {
        if (file.contains(".html")) {
            response.setContentType("text/html;charset=UTF-8");
        } else if (file.contains(".css")) {
            response.setContentType("text/css;charset=UTF-8");
        } else if (file.contains("js")) {
            response.setContentType("application/javascript;charset=UTF-8");
        }
    }

    private void copy(final InputStream s, final OutputStream os) throws IOException {
        final byte[] buffer = new byte[1024];
        int read;
        while ((read = s.read(buffer)) >= 0) {
            os.write(buffer, 0, read);
        }
    }

    private void sendError(final int code, final String string, final HttpServletResponse response) throws IOException {
        response.sendError(code, string);
    }

    private void sendAdminPage(final HttpServletResponse response) throws IOException {
        try (PrintWriter w = response.getWriter()) {
            w.println("<h1>Experiments</h1>");
            w.println("<table>");
            for (final Experiment e : Experiments.getInstance().getExperiments()) {
                w.print("<tr>");
                w.print("<td>" + e.getId() + "</td>");
                w.print("<td>" + e.getState() + "</td>");
                w.print("<td>" + e.getLastStateChange() + "</td>");
                w.println("</tr>");
            }
            w.println("</table>");
            w.println("Current time: " + new Date() + "<br/>");
            w.println("<h1>Treatment statistics</h1>");
            w.println("<table>");
            for (final Entry<TreatmentCombination, TreatmentUsageData> e : this.treatments.getEntries().entrySet()) {
                w.print("<tr>");
                w.print("<td>" + e.getKey() + "</td>");
                w.print("<td>" + e.getValue() + "</td>");
                w.println("</tr>");
            }
            w.println("</table>");
        }
    }

    public static void main(final String[] args) throws Exception {
        Velocity.init();

        final ServerMain m = new ServerMain("asdf");
        Spark.before((final Request request, final Response response) -> DataLog.log(0, "calling URL " + request.url()));
        m.staticFile("/", "/index.html");
        m.staticFile("/index.html");
        m.staticFile("/experiment.js");
        m.staticFile("/experiment.css");
        m.staticFile("/codemirror.js");
        m.staticFile("/codemirror.css");
        m.staticFile("/jquery.min.js");
        Spark.get("/assessment/*/start.html", m::assessmentStart);
        Spark.post("/assessment/*/step/*", m::assessmentStep);
        Spark.get("/shutdown/asdrsqer1223as", m::shutdown);
    }

    private Object assessmentStart(final Request request, final Response response) {
    	long id;
    	try {
    		id = Long.parseLong(request.splat()[0]);
    	} catch (final NumberFormatException e) {
    		return Spark.halt(404);
    	}
    	final AssessmentSuite a = this.assessments.get(id);
    	if (a == null) {
    		return Spark.halt(404);
    	}

    	final Map<String, Object> data = new HashMap<>();
    	data.put("assessment", a);
    	return this.velocity(data, "/start.html.vm");
    }

    private Object assessmentStep(final Request request, final Response response) {
    	long id;
    	try {
    		id = Long.parseLong(request.splat()[0]);
    	} catch (final NumberFormatException e) {
    		return Spark.halt(404);
    	}
    	final AssessmentSuite a = this.assessments.get(id);
    	if (a == null) {
    		return Spark.halt(404);
    	}

    	a.handleResultForCurrentStep(request);

    	int step;
    	try {
    		step = Integer.parseInt(request.splat()[1]);
    	} catch (final NumberFormatException e) {
    		return Spark.halt(404);
    	}

    	if (!a.isNextStep(step)) {
    		DataLog.log(a.getId(), "Invalid step " + step);
    		return "Ungültige Schrittnummer. Bitte nutzen Sie nur die Navigationsfunktionen der Webseite, nicht die des Browsers.";
    	}
    	a.setCurrentStep(step);

    	final AssessmentItem item = a.getStep(step);
    	final Map<String, Object> data = new HashMap<>();
    	data.put("assessment", a);
    	if (item != null) {
    		data.put("nextStep", step + 1);
    		data.put("item", item);
    		return this.velocity(data, item.getTemplate());
    	} else {
    		return this.velocity(data, "/closing.html.vm");
    	}
    }

    private Object velocity(final Map<String, Object> data, final String template) {
    	return new VelocityTemplateEngine().render(new ModelAndView(data, template));
    }

    private void staticFile(final String path) {
    	this.staticFile(path, path);
	}

    private void staticFile(final String urlPath, final String cpPath) {
    	Spark.get(urlPath, (final Request request, final Response response) -> this.getStaticFile(request, response, cpPath));
    }

	private Object getStaticFile(final Request request, final Response response, final String cpPath) throws IOException {
		this.sendFile(cpPath, response.raw());
		return null;
	}

	private void initTreatments() throws IOException {
        final File initFile = new File("prefill.txt");
        if (!initFile.exists()) {
            System.out.println("no init file (prefill.txt) found");
            return;
        }

        try (BufferedReader r = new BufferedReader(new FileReader(initFile))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                final String[] parts = line.split("[ \t]+");
                this.treatments.add(
                        new TreatmentCombination(
                                        this.parseTreatment(parts[0]),
                                        this.parseTreatment(parts[1]),
                                        this.parseDiff(parts[2]),
                                        this.parseDiff(parts[3])),
                        this.parseCount(parts[4]),
                        this.parseReviewScoreSum(parts[5]));
            }
        }
    }

    private Treatment parseTreatment(final String string) {
        return Treatment.valueOf(this.withoutComma(string));
    }

    private Diff parseDiff(final String string) {
        return Diff.valueOf(this.withoutComma(string));
    }

    private String withoutComma(final String string) {
        return string.replace(",", "");
    }

    private int parseReviewScoreSum(final String string) {
        final String[] parts = this.withoutComma(string).split("=");
        if (!parts[0].equals("reviewScoreSum")) {
            throw new RuntimeException("expected reviewScoreSum but was " + parts[0]);
        }
        return Integer.parseInt(parts[1]);
    }

    private int parseCount(final String string) {
        final String[] parts = this.withoutComma(string).split("=");
        if (!parts[0].equals("count")) {
            throw new RuntimeException("expected count but was " + parts[0]);
        }
        return Integer.parseInt(parts[1]);
    }

    private Object shutdown(final Request request, final Response response) {
    	new Thread("stopper") {
    		@Override
			public void run() {
    	    	Spark.stop();
    	        Spark.awaitStop();
    	        try {
					DataLog.close();
				} catch (final IOException e) {
				}
    		}
    	} .start();
    	return "Stopping ...";
    }

}
