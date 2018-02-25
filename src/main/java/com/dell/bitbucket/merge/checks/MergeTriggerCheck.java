package com.dell.bitbucket.merge.checks;

import com.atlassian.bitbucket.hook.repository.*;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;


import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import com.atlassian.bitbucket.hook.repository.*;
import com.atlassian.bitbucket.i18n.I18nService;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

@Component("MergeTriggerCheck")
public class MergeTriggerCheck implements RepositoryMergeCheck {

    private final I18nService i18nService;
    private final PermissionService permissionService;

    @Autowired
    public MergeTriggerCheck(@ComponentImport I18nService i18nService,
                             @ComponentImport PermissionService permissionService) {
        this.i18nService = i18nService;
        this.permissionService = permissionService;
    }

    @Nonnull
    @Override
    public RepositoryHookResult preUpdate(@Nonnull PreRepositoryHookContext context,
                                          @Nonnull PullRequestMergeHookRequest request) {
        Repository repository = request.getPullRequest().getToRef().getRepository();
        String url = "http://dockerlogin-eqx-01.force10networks.com:8080/mergeTrigger.php";
        String branch = request.getPullRequest().getFromRef().getDisplayId();
        String packageName = repository.getName();
        String packageRevision = "4.0.0.84";
        long lpullRequestId = request.getPullRequest().getId();
        String pullRequestId = String.valueOf(lpullRequestId);
        String jsonContainer = "nada";
        String mergeUser = "Administrator";
        String newBranch = branch.replace("/", "!!!");
        char ampresand = '&';
        String myParams = String.format ("branch=%s%c", newBranch, ampresand);
        //String myParams2= String.format("packageName=%s%cpackageRevision=%s%cjsonContainer=%s%cmergeUser=%s%cpullRequestId=%d", packageName, ampresand, packageRevision, ampresand, jsonContainer, ampresand, mergeUser, ampresand, pullRequestId);
        //String myCommand = "curl -d '" + myParams + myParams2 + "' -X POST http://dockerlogin-eqx-01.force10networks.com:8080/mergeTrigger.php";
        //System.out.println(myCommand);

        //String myCommand = "curl -d 'branch=testing&packageName=l2-services&packageRevision=4.0.0.84&jsonContainer=nada&mergeUser=pgits&pullRequestId' -X POST http://dockerlogin-eqx-01.force10networks.com:8080/mergeTriggerTest.php";

        String charset = "UTF-8";  // Or in Java 7 and later, use the constant: java.nio.charset.StandardCharsets.UTF_8.name()

        String query = null;
        try {
            query = String.format("emptyParameter=nothing&my_branch=%s&packageName=%s&packageRevision=%s&jsonContainer=%s&mergeUser=%s&pullRequestId=%s",
                        URLEncoder.encode(branch, charset),
                        URLEncoder.encode(packageName, charset),
                        URLEncoder.encode(packageRevision, charset),
                        URLEncoder.encode(jsonContainer, charset),
                        URLEncoder.encode(mergeUser, charset),
                        URLEncoder.encode(pullRequestId, charset)
                    );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {

            //URLConnection connection = new URL(url + "?" + query).openConnection();
            URLConnection connection = new URL(url).openConnection();
            HttpURLConnection http = (HttpURLConnection)connection;
            http.setRequestMethod("POST");
            http.setDoOutput(true);

            Map<String,String> arguments = new HashMap<>();
            arguments.put("my_branch", branch);
            arguments.put("packageName", packageName);
            arguments.put("packageRevision", packageRevision);
            arguments.put("jsonContainer", jsonContainer);
            arguments.put("mergeUser", mergeUser);
            arguments.put("pullRequestId", pullRequestId);

            StringJoiner sj = new StringJoiner("&");
            for(Map.Entry<String,String> entry : arguments.entrySet())
                sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "="
                        + URLEncoder.encode(entry.getValue(), "UTF-8"));
            byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
            int length = out.length;

            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            http.setRequestProperty("Accept-Charset", charset);
            http.connect();
            try(OutputStream os = http.getOutputStream()) {
                os.write(out);
            }


            java.io.InputStream response = http.getInputStream();

            java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(response));
            // And print each line
            String s = null;
            StringBuilder returnResults = new StringBuilder();
            Boolean returnValue = false;
            while ((s = reader.readLine()) != null) {
                System.out.println(s);
                returnResults.append(s);
            }
            http.disconnect();
            if(returnResults.toString().contains("MergeCheckPassed"))
                if(returnResults.toString().contains("succeeded"))
                    return RepositoryHookResult.accepted();
        }catch(MalformedURLException me){
            me.printStackTrace();
        }catch(Exception io){
            io.printStackTrace();
        }

/*
        try {
            if(shellTest(myCommand))
                return RepositoryHookResult.accepted();
        } catch (java.io.IOException io) {
            System.out.println("io exception = " + io.getMessage());
        } catch (java.lang.InterruptedException ie) {
            System.out.println("interruptedException " + ie.getMessage());
        }
 */
        String summaryMsg  = "checking pgits implementation";
        String detailedMsg = i18nService.getText("com.dell.bitbucket.merge.check",
                "repo hasn't finished MergeTrigger Build and MergeTrigger Smoke Test yet, please try later");
        return RepositoryHookResult.rejected(summaryMsg, detailedMsg);
    }
        /*
        if (!permissionService.hasRepositoryPermission(repository, Permission.REPO_ADMIN)) {
            String summaryMsg = i18nService.getText("mycompany.plugin.merge.check.notrepoadmin.summary",
                    "Only repository administrators may merge pull requests");
            String detailedMsg = i18nService.getText("mycompany.plugin.merge.check.notrepoadmin.detailed",
                    "The user merging the pull request must be an administrator of the target repository");
            return RepositoryHookResult.rejected(summaryMsg, detailedMsg);
        }
        return RepositoryHookResult.accepted();
        */
    public boolean shellTest(String command) throws java.io.IOException, java.lang.InterruptedException {
        java.lang.Runtime rt = java.lang.Runtime.getRuntime();
        // Start a new process: UNIX command ls
        java.lang.Process p = rt.exec(command);
        // You can or maybe should wait for the process to complete

        /*
        p.waitFor();
        System.out.println("Process exited with code" + p.exitValue());
        // Get process' output: its InputStream
        */
        java.io.InputStream is = p.getInputStream();
        java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(is));
        // And print each line
        String s = null;
        StringBuilder returnResults = new StringBuilder();
        Boolean returnValue = false;
        while ((s = reader.readLine()) != null) {
            System.out.println(s);
            returnResults.append(s);
        }
        if(returnResults.toString().contains("MergeCheckPassed"))
            if(returnResults.toString().contains("succeeded"))
                returnValue = true;
        is.close();
        return returnValue;
    }

}

/*
    public RepositoryHookResult preUpdate(@Nonnull PreRepositoryHookContext context,
                                          @Nonnull PullRequestMergeHookRequest request) {
        Repository repository = request.getPullRequest().getToRef().getRepository();
        String url = "http://dockerlogin-eqx-01.force10networks.com:8080/mergeTrigger.php";
        String branch = request.getPullRequest().getFromRef().getDisplayId();
        String packageName = repository.getName();
        String packageRevision = "4.0.0.84";
        long pullRequestId = request.getPullRequest().getId();
        String jsonContainer = "nada";
        String mergeUser = "Administrator";
        String newBranch = branch.replace("/", "!!!");
        char ampresand = '&';
        String myParams = String.format ("branch=%s%c", newBranch, ampresand);
        String myParams2= String.format("packageName=%s%cpackageRevision=%s%cjsonContainer=%s%cmergeUser=%s%cpullRequestId=%d", packageName, ampresand, packageRevision, ampresand, jsonContainer, ampresand, mergeUser, ampresand, pullRequestId);
        String myCommand = "curl -d '" + myParams + myParams2 + "' -X POST http://dockerlogin-eqx-01.force10networks.com:8080/mergeTrigger.php";
        System.out.println(myCommand);

        //String myCommand = "curl -d 'branch=testing&packageName=l2-services&packageRevision=4.0.0.84&jsonContainer=nada&mergeUser=pgits&pullRequestId' -X POST http://dockerlogin-eqx-01.force10networks.com:8080/mergeTriggerTest.php";
        try {
            if(shellTest(myCommand))
                return RepositoryHookResult.accepted();
        } catch (java.io.IOException io) {
            System.out.println("io exception = " + io.getMessage());
        } catch (java.lang.InterruptedException ie) {
            System.out.println("interruptedException " + ie.getMessage());
        }
        String summaryMsg  = "checking pgits implementation";
        String detailedMsg = i18nService.getText("com.dell.bitbucket.merge.check",
                "repo hasn't finished mergeTrigger build and smoke test yet, please try later");
        return RepositoryHookResult.rejected(summaryMsg, detailedMsg);
    }

 */