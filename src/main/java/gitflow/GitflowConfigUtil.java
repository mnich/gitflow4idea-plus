package gitflow;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.config.GitConfigUtil;
import git4idea.repo.GitRepository;
import gitflow.ui.NotifyUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 * @author Opher Vishnia / opherv.com / opherv@gmail.com
 */

public class GitflowConfigUtil {

    // Old git-flow AVH config keys
    public static final String BRANCH_MASTER = "gitflow.branch.master";
    public static final String BRANCH_DEVELOP = "gitflow.branch.develop";
    public static final String PREFIX_FEATURE = "gitflow.prefix.feature";
    public static final String PREFIX_RELEASE = "gitflow.prefix.release";
    public static final String PREFIX_HOTFIX = "gitflow.prefix.hotfix";
    public static final String PREFIX_BUGFIX = "gitflow.prefix.bugfix";
    public static final String PREFIX_SUPPORT = "gitflow.prefix.support";
    public static final String PREFIX_VERSIONTAG = "gitflow.prefix.versiontag";

    // New git-flow-next config keys
    public static final String BRANCH_FEATURE_PREFIX = "gitflow.branch.feature.prefix";
    public static final String BRANCH_RELEASE_PREFIX = "gitflow.branch.release.prefix";
    public static final String BRANCH_HOTFIX_PREFIX = "gitflow.branch.hotfix.prefix";
    public static final String BRANCH_BUGFIX_PREFIX = "gitflow.branch.bugfix.prefix";
    public static final String BRANCH_SUPPORT_PREFIX = "gitflow.branch.support.prefix";

    private static  Map<Project, Map<String, GitflowConfigUtil>> gitflowConfigUtilMap = new HashMap<Project, Map<String, GitflowConfigUtil>>();

    Project project;
    GitRepository repo;
    public String masterBranch;
    public String developBranch;
    public String featurePrefix;
    public String releasePrefix;
    public String hotfixPrefix;
    public String bugfixPrefix;
    public String supportPrefix;
    public String versiontagPrefix;

    public static GitflowConfigUtil getInstance(@NotNull Project project_, @NotNull GitRepository repo_)
    {
        GitflowConfigUtil instance;
        if (gitflowConfigUtilMap.containsKey(project_) && gitflowConfigUtilMap.get(project_).containsKey(repo_.getPresentableUrl())) {
            instance = gitflowConfigUtilMap.get(project_).get(repo_.getPresentableUrl());
        } else {
            Map<String, GitflowConfigUtil> innerMap = new HashMap<String, GitflowConfigUtil>();
            instance = new GitflowConfigUtil(project_, repo_);

            gitflowConfigUtilMap.put(project_, innerMap);
            innerMap.put(repo_.getPresentableUrl(), instance);

            //cleanup
            Disposer.register(repo_, () -> innerMap.remove(repo_));
            Disposer.register(project_, () -> gitflowConfigUtilMap.remove(project_));
        }

        return instance;
    }

    GitflowConfigUtil(Project project_, GitRepository repo_){
        project = project_;
        repo = repo_;

        update();
    }

    /**
     * Detects git-flow-next branch names by scanning all local branches.
     * Looks for branches with type=base in git config.
     * Uses the parent field to distinguish: production (no parent) vs develop (has parent).
     */
    private void detectGitflowNextBranches() {
        VirtualFile root = repo.getRoot();

        try {
            String developCandidate = null;
            String productionCandidate = null;

            // Get all local branches and check their git config
            java.util.Collection<git4idea.GitLocalBranch> localBranches = repo.getBranches().getLocalBranches();

            for (git4idea.GitLocalBranch branch : localBranches) {
                String branchName = branch.getName();
                String type = GitConfigUtil.getValue(project, root, "gitflow.branch." + branchName + ".type");

                if ("base".equals(type)) {
                    String parent = GitConfigUtil.getValue(project, root, "gitflow.branch." + branchName + ".parent");

                    if (parent == null || parent.isEmpty()) {
                        // This is production branch (no parent)
                        productionCandidate = branchName;
                    } else {
                        // This is develop branch (has parent)
                        developCandidate = branchName;
                        // Also get production from parent if not found yet
                        if (productionCandidate == null) {
                            productionCandidate = parent;
                        }
                    }
                }

                // If we found both, we can stop
                if (productionCandidate != null && developCandidate != null) {
                    break;
                }
            }

            // Set the values
            masterBranch = productionCandidate;
            developBranch = developCandidate;

        } catch (Exception e) {
            // On error, mark as not initialized
            masterBranch = null;
            developBranch = null;
        }
    }

    public void update(){
        VirtualFile root = repo.getRoot();

        try{
            Future<?> f = ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    // Try to detect git-flow-next branches first
                    detectGitflowNextBranches();

                    // If detection didn't find branches, try old AVH format
                    if (masterBranch == null || developBranch == null) {
                        masterBranch = GitConfigUtil.getValue(project, root, BRANCH_MASTER);
                        developBranch = GitConfigUtil.getValue(project, root, BRANCH_DEVELOP);
                    }

                    // Prefixes: try old format first, then new format
                    featurePrefix = GitConfigUtil.getValue(project, root, PREFIX_FEATURE);
                    if (featurePrefix == null) {
                        featurePrefix = GitConfigUtil.getValue(project, root, BRANCH_FEATURE_PREFIX);
                    }

                    releasePrefix = GitConfigUtil.getValue(project, root, PREFIX_RELEASE);
                    if (releasePrefix == null) {
                        releasePrefix = GitConfigUtil.getValue(project, root, BRANCH_RELEASE_PREFIX);
                    }

                    hotfixPrefix = GitConfigUtil.getValue(project, root, PREFIX_HOTFIX);
                    if (hotfixPrefix == null) {
                        hotfixPrefix = GitConfigUtil.getValue(project, root, BRANCH_HOTFIX_PREFIX);
                    }

                    bugfixPrefix = GitConfigUtil.getValue(project, root, PREFIX_BUGFIX);
                    if (bugfixPrefix == null) {
                        bugfixPrefix = GitConfigUtil.getValue(project, root, BRANCH_BUGFIX_PREFIX);
                    }

                    supportPrefix = GitConfigUtil.getValue(project, root, PREFIX_SUPPORT);
                    if (supportPrefix == null) {
                        supportPrefix = GitConfigUtil.getValue(project, root, BRANCH_SUPPORT_PREFIX);
                    }

                    versiontagPrefix = GitConfigUtil.getValue(project, root, PREFIX_VERSIONTAG);
                } catch (VcsException e) {
                    NotifyUtil.notifyError(project, "Config error", e);
                }
            });
            f.get();

        }  catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }


    public String getFeatureNameFromBranch(String branchName){
        return branchName.substring(branchName.indexOf(featurePrefix)+featurePrefix.length(),branchName.length());
    }

    public String getReleaseNameFromBranch(String branchName){
        return branchName.substring(branchName.indexOf(releasePrefix) + releasePrefix.length(), branchName.length());
    }

    public String getHotfixNameFromBranch(String branchName){
        return branchName.substring(branchName.indexOf(hotfixPrefix) + hotfixPrefix.length(), branchName.length());
    }

    public String getBugfixNameFromBranch(String branchName){
        return branchName.substring(branchName.indexOf(bugfixPrefix)+bugfixPrefix.length(),branchName.length());
    }

    public String getRemoteNameFromBranch(String branchName){
        return branchName.substring(0,branchName.indexOf("/"));
    }

    public void setMasterBranch(String branchName)
    {
        masterBranch = branchName;
        VirtualFile root = repo.getRoot();
        try {
            GitConfigUtil.setValue(project, root, BRANCH_MASTER, branchName);
        } catch (VcsException e) {
            NotifyUtil.notifyError(project, "Config error", e);
        }
    }

    public void setDevelopBranch(String branchName) {
        developBranch = branchName;
        VirtualFile root = repo.getRoot();
        try {
            GitConfigUtil.setValue(project, root, BRANCH_DEVELOP, branchName);
        } catch (VcsException e) {
            NotifyUtil.notifyError(project, "Config error", e);
        }
    }

    public void setReleasePrefix(String prefix) {
        releasePrefix = prefix;
        VirtualFile root = repo.getRoot();

        try {
            GitConfigUtil.setValue(project, root, PREFIX_RELEASE, prefix);
        } catch (VcsException e) {
            NotifyUtil.notifyError(project, "Config error", e);
        }
    }

    public void setFeaturePrefix(String prefix) {
        featurePrefix = prefix;
        VirtualFile root = repo.getRoot();

        try {
            GitConfigUtil.setValue(project, root, PREFIX_FEATURE, prefix);
        } catch (VcsException e) {
            NotifyUtil.notifyError(project, "Config error", e);
        }
    }

    public void setHotfixPrefix(String prefix) {
        hotfixPrefix = prefix;
        VirtualFile root = repo.getRoot();

        try {
            GitConfigUtil.setValue(project, root, PREFIX_HOTFIX, prefix);
        } catch (VcsException e) {
            NotifyUtil.notifyError(project, "Config error", e);
        }
    }

    public void setBugfixPrefix(String prefix) {
        bugfixPrefix = prefix;
        VirtualFile root = repo.getRoot();

        try {
            GitConfigUtil.setValue(project, root, PREFIX_BUGFIX, prefix);
        } catch (VcsException e) {
            NotifyUtil.notifyError(project, "Config error", e);
        }
    }

    public void setSupportPrefix(String prefix) {
        supportPrefix = prefix;
        VirtualFile root = repo.getRoot();

        try {
            GitConfigUtil.setValue(project, root, PREFIX_SUPPORT, prefix);
        } catch (VcsException e) {
            NotifyUtil.notifyError(project, "Config error", e);
        }
    }

    public void setVersionPrefix(String prefix) {
        versiontagPrefix = prefix;
        VirtualFile root = repo.getRoot();

        try {
            GitConfigUtil.setValue(project, root, PREFIX_VERSIONTAG, prefix);
        } catch (VcsException e) {
            NotifyUtil.notifyError(project, "Config error", e);
        }
    }

    // this still reads from config because it always runs from an action and never from the EDT
    public String getBaseBranch(String branchName){

        VirtualFile root = repo.getRoot();

        String baseBranch=null;
        try{
            baseBranch = GitConfigUtil.getValue(project, root, "gitflow.branch."+branchName+".base");
        }
        catch (VcsException e) {
            NotifyUtil.notifyError(project, "Config error", e);
        }

        return baseBranch;
    }
}
