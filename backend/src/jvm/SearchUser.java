public class SearchUser {
    private String id;
    private String screenName;
    private String githubHandle;
    private String bio;

    public SearchUser() {}

    public String getId() {
        return id;
    }

    public SearchUser setId(String id) {
        this.id = id;
        return this;
    }

    public String getScreenName() {
        return screenName;
    }

    public SearchUser setScreenName(String screenName) {
        this.screenName = screenName;
        return this;
    }

    public String getGithubHandle() {
        return githubHandle;
    }

    public SearchUser setGithubHandle(String githubHandle) {
        this.githubHandle = githubHandle;
        return this;
    }

    public String getBio() {
        return bio;
    }

    public SearchUser setBio(String bio) {
        this.bio = bio;
        return this;
    }

}
