package gsn.http.ac;

import org.apache.log4j.Logger;


public class UserUtils {

    private static transient Logger logger = Logger.getLogger(UserUtils.class);

    /*
   * Creates a User object following credentials in access control
   * Returns null, if user not registered or password is incorrect
   * */
    public static User allowUserToLogin(String username, String password) {
        User user = null;
        ConnectToDB ctdb = null;

        try {

            ctdb = new ConnectToDB();
            if (ctdb.valueExistsForThisColumnUnderOneCondition(new Column("USERNAME", username), new Column("ISCANDIDATE", "no"), "ACUSER") == true) {
                String enc = Protector.encrypt(password);
                if ((ctdb.isPasswordCorrectForThisUser(username, enc) == false)) {

                    logger.warn("Incorrect password for user : " + username);
                } else {
                    logger.warn("Username and password are correct for user : " + username);
                    user = new User(username, enc, ctdb.getDataSourceListForUserLogin(username), ctdb.getGroupListForUser(username));
                    User userFromBD = ctdb.getUserForUserName(username);
                    user.setLastName(userFromBD.getLastName());
                    user.setEmail(userFromBD.getEmail());
                    user.setFirstName(userFromBD.getFirstName());
                }

            } else {

                logger.warn("This username \""+username+"\" does not exist !");
            }

        } catch (Exception e) {
            logger.warn("Exception caught : " + e.getMessage());
        } finally {
            if (ctdb != null) {
                ctdb.closeStatement();
                ctdb.closeConnection();
            }
        }
        return user;
    }
}
