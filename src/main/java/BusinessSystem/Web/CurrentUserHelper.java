package BusinessSystem.Web;

import BusinessSystem.Model.User;
import BusinessSystem.Security.UserPrincipal;
import org.springframework.http.HttpStatus;

public final class CurrentUserHelper {

    private CurrentUserHelper() {
    }

    public static User require(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return principal.getUser();
    }
}
