/*
 * Copyright [2018] [James Carr]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import org.apache.commons.cli.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * POJO to represent a simple Preservica
 * Collection
 */
class Collection {
    private Integer parent;
    private String name;
    private Integer reference;

    Collection(Integer parent, String name) {
        this.parent = parent;
        this.name = name.trim();
        this.reference = this.name.toLowerCase().hashCode();
    }

    Integer getParent() {
        return parent;
    }

    Integer getReference() {
        return reference;
    }

    String getName() {
        return name.trim();
    }

    public String toString() {
        return (reference + " " + name + "  " + parent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Collection that = (Collection) o;
        return reference != null ? reference.equals(that.reference) : that.reference == null;
    }

    @Override
    public int hashCode() {
        int result = parent != null ? parent.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (reference != null ? reference.hashCode() : 0);
        return result;
    }
}

/**
 * Populate Preservica with Collections based on information
 * in a spreadsheet
 */
public class CreateCollections {

    private CredentialsProvider credsProvider = new BasicCredentialsProvider();
    private HttpClientConnectionManager cm = new BasicHttpClientConnectionManager();
    private CloseableHttpClient httpclient;

    private String parentCollection;
    private String regionURI;


    /**
     *  Parse the command line arguments
     *
     */
    public static void main(String[] args) {

        File inputFile = null;
        Properties properties = null;

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        Boolean testRun = false;

        Options options = new Options();
        options.addOption("i", "input", true, "The csv file containing the collection names");
        options.addOption("c", "config", true, "properties file with credentials");
        options.addOption("d", "dry-run", false, "don't create the collection just print the structure");
        options.addOption("h", "help", false, "print this message");

        final String cmdLine = "collection-loader.cmd -i file.csv [-c config.properties] [--dry-run]";

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("h")) {
                formatter.printHelp(cmdLine, "", options, "\nUse the --dry-run argument to check your csv file syntax");
                System.exit(1);
            }

            if (line.hasOption("d")) {
                testRun = true;
            }

            if (line.hasOption("i")) {
                String inputFileName = line.getOptionValue("i");
                inputFile = new File(inputFileName);
                if (!inputFile.exists()) {
                    System.out.println(String.format("The input csv file name %s does not exist", inputFileName));
                    System.exit(1);
                }
            } else {
                formatter.printHelp(cmdLine, options);
                System.exit(1);
            }

            properties = new Properties();
            if (line.hasOption("c")) {
                String inputFileName = line.getOptionValue("c");
                File configFile = new File(inputFileName);
                if (!inputFile.exists()) {
                    System.out.println(String.format("The config file with name %s does not exist", inputFileName));
                    System.exit(1);
                }

                InputStream is = new FileInputStream(configFile);
                properties.load(is);
                is.close();

                if (!testRun) {
                    if (properties.getProperty("preservica.username").isEmpty()) {
                        System.out.println("add a valid username to the config file");
                        System.exit(1);
                    }
                    if (properties.getProperty("preservica.password").isEmpty()) {
                        System.out.println("add a valid password to the config file");
                        System.exit(1);
                    }
                }


            } else {

                File configFile = new File("config.properties");
                if (!inputFile.exists()) {
                    System.out.println(String.format("The default config file does not exist"));
                    formatter.printHelp(cmdLine, options);
                    System.exit(1);

                } else {
                    InputStream is = new FileInputStream(configFile);
                    properties.load(is);
                    is.close();

                    if (!testRun) {
                        if (properties.getProperty("preservica.username").isEmpty()) {
                            System.out.println("add a valid username to the config file");
                            System.exit(1);
                        }
                        if (properties.getProperty("preservica.password").isEmpty()) {
                            System.out.println("add a valid password to the config file");
                            System.exit(1);
                        }
                    }

                }
            }

            CreateCollections createCollections = new CreateCollections(properties);
            createCollections.create(properties, inputFile, testRun);

        } catch (Exception exp) {
            System.out.println(exp.getMessage());
            formatter.printHelp(cmdLine, options);
        }
    }


    /**
     *  Constructor
     *
     * @param config
     */
    public CreateCollections(Properties config) {
        String username = config.getProperty("preservica.username");
        String password = config.getProperty("preservica.password");
        parentCollection = config.getProperty("preservica.root.collection", "");
        String region = config.getProperty("preservica.region");
        regionURI = String.format("https://%s.preservica.com/api", region.toLowerCase());
        credsProvider.setCredentials(
                new AuthScope(AuthScope.ANY),
                new UsernamePasswordCredentials(username, password));
    }

    /**
     * Get the http client for the REST calls.
     *
     * @return HttpClient
     */
    private CloseableHttpClient getClient() {
        if (httpclient == null) {
            httpclient = HttpClients.custom().setConnectionManager(cm).setDefaultCredentialsProvider(credsProvider).build();
        }
        return httpclient;
    }


    /**
     * Read the CSV file and create a list of collections
     *
     * @param config
     * @param csvFile
     * @param testRun
     * @throws Exception
     */
    private void create(Properties config, File csvFile, Boolean testRun) throws Exception {

        BOMInputStream bis = new BOMInputStream(new FileInputStream(csvFile));
        Reader reader = new InputStreamReader(bis);
        CSVParser parser = CSVFormat.DEFAULT.parse(reader);
        Map<String, Integer> headerMap = parser.getHeaderMap();
        Set<Collection> collections = new LinkedHashSet<>();
        Integer parent = null;
        for (CSVRecord row : parser) {
            Iterator<String> col = row.iterator();
            parent = null;
            for (String column : row) {
                if (!column.isEmpty()) {
                    Collection collection = new Collection(parent, column.trim());
                    parent = collection.getReference();
                    collections.add(collection);
                }
            }
        }
        reader.close();
        bis.close();

        /**
         *  Print the results to the screen
         *  or actually create the collections.
         */
        if (testRun) {
            for (Collection c : collections) {
                if (c.getParent() == null) {
                    printTree(collections, c, 0);
                }
            }
        } else {
            for (Collection c : collections) {
                if (c.getParent() == null) {
                    createCollections(collections, c, parentCollection);
                }
            }
        }
    }


    /**
     * Create the Preservica collections.
     *
     * @param tree
     * @param root
     * @param parentRef
     */
    private void createCollections(Set<Collection> tree, Collection root, String parentRef) {

        String ref;
        if (parentRef == null || parentRef.isEmpty()) {
            ref = createServerSideCollection(root, "@root@");
        } else {
            ref = createServerSideCollection(root, parentRef);
        }
        System.out.println("Created Collection: " + root.getName() + " :" + ref);
        List<Collection> children = selectChildren(tree, root.getReference());
        for (Collection child : children) {
            createCollections(tree, child, ref);
        }
    }


    private String prefix(int level) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < level; i++) {
            s.append("----|");
        }
        return s.toString();
    }

    private List<Collection> selectChildren(Set<Collection> tree, int parentId) {
        List<Collection> result = new ArrayList<>();
        for (Collection d : tree) {
            if (d.getParent() != null) {
                if (d.getParent() == parentId) {
                    result.add(d);
                }
            }
        }
        return result;
    }

    /**
     * Print the results on screen for testing
     *
     * @param tree
     * @param root
     * @param level
     */
    private void printTree(Set<Collection> tree, Collection root, int level) {
        System.out.print(prefix(level));
        System.out.println(root.getName());
        List<Collection> children = selectChildren(tree, root.getReference());

        for (Collection child : children) {
            printTree(tree, child, level + 1);
        }
    }

    public String createServerSideCollection(Collection collection, String parentRef) {

        CloseableHttpClient client = getClient();
        CloseableHttpResponse response = null;

        List<NameValuePair> parameters = new ArrayList<>();

        try {
            HttpPost post = new HttpPost(String.format("%s/entity/collections/", regionURI));
            post.setHeader("Content-Type", URLEncodedUtils.CONTENT_TYPE);

            parameters.add(new BasicNameValuePair("collectionCode", collection.getName()));
            parameters.add(new BasicNameValuePair("parentRef", parentRef));
            parameters.add(new BasicNameValuePair("securityTag", "open"));
            parameters.add(new BasicNameValuePair("title", collection.getName()));

            String data = URLEncodedUtils.format(parameters, Charset.defaultCharset());
            StringEntity se = new StringEntity(data);
            post.setEntity(se);
            response = client.execute(post);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                StringWriter sw = new StringWriter();
                IOUtils.copy(response.getEntity().getContent(), sw);
                int pos1 = StringUtils.indexOf(sw.toString(), "<CollectionRef>");
                int pos2 = StringUtils.indexOf(sw.toString(), "</CollectionRef>");
                return sw.toString().substring(pos1 + 15, pos2);
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                System.out.println("Failed to create new collection");
                System.out.println(response.getStatusLine().toString());
                System.exit(1);
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        } finally {
            EntityUtils.consumeQuietly(response.getEntity());
            IOUtils.closeQuietly(response);
        }
        return null;
    }

}
