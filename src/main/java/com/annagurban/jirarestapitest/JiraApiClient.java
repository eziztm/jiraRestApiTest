package com.annagurban.jirarestapitest;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.JiraRestClientFactory;
import com.atlassian.jira.rest.client.OptionalIterable;
import com.atlassian.jira.rest.client.domain.BasicIssue;
import com.atlassian.jira.rest.client.domain.Comment;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.domain.IssueType;
import com.atlassian.jira.rest.client.domain.Project;
import com.atlassian.jira.rest.client.domain.Transition;
import com.atlassian.jira.rest.client.domain.User;
import com.atlassian.jira.rest.client.domain.Visibility;
import com.atlassian.jira.rest.client.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.domain.input.TransitionInput;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A simple client for JIRA REST API. Ref: the Java Jira Rest Client
 * documentation
 */
public class JiraApiClient {

    private static final String JIRA_URL = "http://localhost:8081/";
    private static final String JIRA_ADMIN_USERNAME = "admin";
    private static final String JIRA_ADMIN_PASSWORD = "password";

    public static void main(String... args) throws URISyntaxException {

        // Construct the JRJC client
        System.out.println(String.format("Logging in to %s with username '%s' and password '%s'", JIRA_URL, JIRA_ADMIN_USERNAME, JIRA_ADMIN_PASSWORD));
        JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        URI uri = new URI(JIRA_URL);
        JiraRestClient client = factory.createWithBasicHttpAuthentication(uri, JIRA_ADMIN_USERNAME, JIRA_ADMIN_PASSWORD);


        // Invoke the JRJC Client
        Promise<User> promise = client.getUserClient().getUser("ezizconduct");
        User user = promise.claim();

        // Print the result
        System.out.println(String.format("Your admin user's email address is: %s\r\n", user.getEmailAddress()));

        //Retrieve the project
        Promise<Project> promiseProject = client.getProjectClient().getProject("TEST");
        Project project = promiseProject.claim();

        System.out.println(String.format("Retrieved a project [%s] %s", project.getKey(), project.getName()));

        // Retrieve metadata: issue type
        Iterable<IssueType> issueTypesMetadata = client.getMetadataClient().getIssueTypes().claim();

        System.out.println("Issue types");
        issueTypesMetadata.forEach(it -> System.out.println(it.getId() + ": " + it.getDescription()));
        System.out.println("");

        // Create a new issue
        OptionalIterable<IssueType> issueTypes = project.getIssueTypes();
        String issueKey = "TEST-5";
        if (issueTypes.iterator().hasNext()) {
            IssueType itype = issueTypes.iterator().next();

            System.out.println("Adding a new issue with type " + itype.getName());

            IssueInputBuilder iib = new IssueInputBuilder(project, itype);
            iib.setSummary("New Issue created from client API");
            iib.setAssignee(user);
            iib.setDescription("New Issue created from client API");

            Promise<BasicIssue> pbi = client.getIssueClient().createIssue(iib.build());
            BasicIssue bi = pbi.claim();
            issueKey = bi.getKey();

            System.out.println(String.format("Created an issue [%s] %s", bi.getKey(), bi.getSelf()));
        }

        //Getting an issue
        Promise<Issue> issuePromise = client.getIssueClient().getIssue(issueKey);
        Issue issue = issuePromise.claim();

        System.out.println(String.format("Retrieved an issue %s", issue.getKey()));

        //Retrieve custom field names
        System.out.println("\nCustom field: " + issue.getFieldByName("customfield_10001"));

        //Add a comment in issue
        Comment comment = new Comment(null, "This is a comment from the api client", user, user, null, null, new Visibility(Visibility.Type.ROLE, "Users"), null);
        client.getIssueClient().addComment(issue.getCommentsUri(), comment);

        //Update the issue description
        System.out.println("\nCurrent issue description is " + issue.getDescription());

        //TODO
        //Change Status
        System.out.println("\nCurrent issue status is " + issue.getStatus());

        // Retrieves a list of transition of the issue. The transition ID will be needed to change Issue status
        Iterable<Transition> transitions = client.getIssueClient().getTransitions(issue).claim();
        transitions.forEach(t -> System.out.println("Transition " + t.getId() + ": " + t.getName()));

        client.getIssueClient().transition(issue, new TransitionInput(11));
        System.out.println("Changed issue status");
    }

}
