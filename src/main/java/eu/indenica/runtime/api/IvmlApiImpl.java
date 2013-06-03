/**
 * 
 */
package eu.indenica.runtime.api;

import java.net.URL;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jws.WebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import at.ac.tuwien.infosys.util.Util;
import at.ac.tuwien.infosys.ws.DynamicWSClient;
import eu.indenica.runtime.Constants;
import eu.indenica.runtime.dto.Data;
import eu.indenica.runtime.dto.Filter;
import eu.indenica.runtime.services.impl.RepositoryImpl;

/**
 * @author Christian Inzinger
 * 
 */
@WebService(endpointInterface = "eu.indenica.runtime.api.IIvmlApi")
public class IvmlApiImpl extends ApiPlugin implements IIvmlApi {
    private static final Logger LOG = LoggerFactory
            .getLogger(IvmlApiImpl.class);
    private static final Util util = Util.getInstance();
    private static final Pattern projectNamePattern = Pattern
            .compile(".*project\\s*([\\S]*)\\s*\\{.*");

    /**
     * @param repositoryWsdl
     */
    public IvmlApiImpl(URL repositoryWsdl) {
        super(repositoryWsdl);
    }

    /**
     * @see eu.indenica.runtime.api.IIvmlApi#storeProject(de.uni_hildesheim.sse.ivml.Project)
     */
    @Override
    public void storeModel(String model) {
        try {
            // LOG.trace("Model: {}", model);
            Matcher nameMatcher = projectNamePattern.matcher(model);
            if(!nameMatcher.find())
                throw new IllegalArgumentException(
                        "Supplied model string not valid: " + model);

            String name = nameMatcher.group(1);
            LOG.debug("Storing model {}", name);
            Data projectData = new Data();
            projectData.value =
                    util.xml.toElement(new StringBuilder().append("<")
                            .append(Constants.DATA_VARIABILITY).append(">")
                            .append("<name>").append(name).append("</name>")
                            .append("<content>").append(model)
                            .append("</content>").append("</")
                            .append(Constants.DATA_VARIABILITY).append(">")
                            .toString());
            repository.publishData(projectData);
        } catch(Exception e) {
            LOG.error("Could not store model", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @see eu.indenica.runtime.api.IIvmlApi#getProject(java.lang.String)
     */
    @Override
    public String getModel(String name) {
        LOG.debug("Retrieving model for '{}'", name);
        Filter nameFilter = new Filter();
        try {
            nameFilter.value =
                    util.xml.toElement(new StringBuilder().append("<")
                            .append(Constants.DATA_VARIABILITY).append(">")
                            .append("<name>").append(name).append("</name>")
                            .append("</").append(Constants.DATA_VARIABILITY)
                            .append(">").toString());
        } catch(Exception e) {
            LOG.error("Could not create filter to retrieve data!", e);
            throw new RuntimeException(e);
        }
        Data data = repository.getData(nameFilter);
        return ((Element) data.value).getElementsByTagName("content").item(0)
                .getTextContent();
    }

    public static void main(String[] args) throws Exception {
        Logger LOG = LoggerFactory.getLogger("ivmlapi-main");
        String repoUrl = "http://0.0.0.0:45689/repo";
        URL repoWsdl = new URL(repoUrl + "?wsdl");
        {
            LOG.info("Starting repository...");
            RepositoryImpl r = new RepositoryImpl();
            r.deploy(repoUrl);
        }

        String apiUrl = "http://0.0.0.0:45690/ivml-api";
        URL apiWsdl = new URL(apiUrl + "?wsdl");
        {
            LOG.info("Starting ivml API...");
            IvmlApiImpl api = new IvmlApiImpl(repoWsdl);
            api.deploy(apiUrl);
        }

        IIvmlApi client =
                DynamicWSClient.createClientJaxws(IIvmlApi.class, apiWsdl);
        {
            LOG.info("Storing resolved variability model...");
            client.storeModel(new Scanner(IvmlApiImpl.class.getClassLoader()
                    .getResourceAsStream("PL_SimElevator_0.ivml"))
                    .useDelimiter("\\Z").next());
        }

        {
            LOG.info("Retrieving variability model...");
            String model = client.getModel("PL_SimElevator");
            LOG.debug("Got model: {}...", model.split("\\n")[0]);
        }

        System.in.read();
        System.exit(0);
    }
}
