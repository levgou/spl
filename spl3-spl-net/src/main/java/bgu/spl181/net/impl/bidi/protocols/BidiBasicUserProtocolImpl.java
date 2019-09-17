package bgu.spl181.net.impl.bidi.protocols;

import bgu.spl181.net.api.bidi.BidiMessagingProtocol;
import bgu.spl181.net.api.bidi.Connections;
import bgu.spl181.net.dataholders.UsersData;
import bgu.spl181.net.impl.bidi.ConnectionsImpl;
import bgu.spl181.net.json.AccessLimiter;
import bgu.spl181.net.json.GenericUserDBInterface;
import bgu.spl181.net.types.DB;
import bgu.spl181.net.types.MsgType;
import bgu.spl181.net.types.PackType;
import bgu.spl181.net.srv.BaseServer;
import bgu.spl181.net.dataholders.CmdPackage;
import bgu.spl181.net.exceptions.CmdParseException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class BidiBasicUserProtocolImpl implements BidiMessagingProtocol<String> {

    // TODO: 1/10/18 could hold read data in mem to improve performance

    protected int connectionId;
    protected ConnectionsImpl<String> connections;
    protected Logger logger = BaseServer.logger;
    protected UsersData ud;
    protected static final Object broadcastLock = new Object();
    // TODO: 1/10/18 rethink lock above

    public BidiBasicUserProtocolImpl(UsersData ud) {
        this.ud = ud;
    }

    @Override
    public void start(int connectionId, Connections<String> connections) {

        this.connectionId = connectionId;
        this.connections = (ConnectionsImpl<String>) connections;
        ud.addClient(connectionId);

    }


    @Override
    public void process(String message) {
        CmdPackage messagePack = genCmdPackage(message, PackType.CLIENT_MSG);
        logger.info(String.format("Processing Client msg | type: %s | args: %s ",
                messagePack.type.name(), Arrays.toString(messagePack.args)));

        CmdPackage processedAns;
        // sync on broadcast Lock to prevent race-condition between SIGNOUT & Broadcast
        // TODO: 1/10/18 rethink
//        synchronized (broadcastLock) {
            processedAns = processMessage(messagePack);
//        }

        String answer;

        if (processedAns == null) {
            logger.info(String.format("Processed answer for | CMD: <%s> | Args: <%s> | is null",
                    messagePack.type.name(), Arrays.toString(messagePack.args)));
            return;
        } else {
            logger.info(String.format("Processed answer for \n| CMD: <%s> | Args: <%s> |\n is -> | CMD: <%s> | Args: <%s> |",
                    messagePack.type.name(), Arrays.toString(messagePack.args),
                    processedAns.type.name(), Arrays.toString(processedAns.args)));
        }

        answer = CmdPackage.assembleMsg(processedAns);
        if (connections.send(connectionId, answer)) {
            logger.info(String.format("Successfully sent message <%s> to Client%d", answer, connectionId));
        } else {
            logger.warn(String.format("Failed sending message <%s> to Client%d", answer, connectionId));
        }
    }


    protected CmdPackage processMessage(CmdPackage messagePack) {

        CmdPackage msgAns = null;
        if (messagePack.type == MsgType.LOGIN) {
            msgAns = handleLogin(messagePack.args);

        } else if (messagePack.type == MsgType.REGISTER) {
            msgAns = handleRegister(messagePack.args);

        } else if (messagePack.type == MsgType.SIGNOUT) {
            msgAns = handleSignout();

        }
        // Request
        else {
            msgAns = handleRequest(messagePack.args);
        }
        return msgAns;
    }


    protected CmdPackage genCmdPackage(String message, PackType packType) {
        CmdPackage messagePack = null;
        try {
            messagePack = new CmdPackage(message, packType);
        } catch (CmdParseException e) {
            logger.fatal(ExceptionUtils.getStackTrace(e));
            logger.info("Send client error message!");
            try {
                messagePack = new CmdPackage("ERROR - " + e.getMessage(), PackType.SERVER_RES);
            } catch (CmdParseException explosion) {
                logger.fatal("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                logger.fatal(ExceptionUtils.getStackTrace(explosion));
                logger.fatal("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                return messagePack;
            }
        }

        return messagePack;
    }

    //------------------------------------------------------------------------------------------------------------------
    // ----------------------------------------------- Message handlers-------------------------------------------------

    /**
     * Try to sign out - fail iff didn't sign in
     *
     * @return Acknowledge package - signout success/fail
     * @db_acess: None
     */
    protected CmdPackage handleSignout() {
        Boolean clientLogStatus = ud.isClientLogged(connectionId);
        if (clientLogStatus == null || clientLogStatus == false) {
            logger.warn(String.format("Client%d not logged in", connectionId));
            return genErrorToClient("signout failed");
        }

        ud.logout(connectionId);
        logger.warn(String.format("Client%d signed out", connectionId));
        return genAckToClient("signout succeeded");

    }


    /**
     * <b>db_acess:</b><p>
     * Read: Users <p>
     * Write: None
     *
     * @param args - Credentials for Login
     * @return <b>Fail-Package:</b> <p>
     * > If user name - already logged in <p>
     * > If Client ID already logged in with some user <p>
     * > If username/password is incorrect <p>
     * <p>
     * <b>Ack-Package: </b><p>
     * > If all of the above are false
     */
    protected CmdPackage handleLogin(String[] args) {
        String username = args[0];
        String pw = args[1];
        CmdPackage proc_status = null;

        AccessLimiter.getReadAccess(DB.USER);

        if (ud.isClientLogged(connectionId)) {
            logger.warn(String.format("Client%d already logged in", connectionId));
            proc_status = genErrorToClient("login failed");
        } else if (ud.userLogged(username)) {
            logger.warn(String.format("User %s already logged in", username));
            proc_status = genErrorToClient("login failed");
        } else if (!validUserPWcombo(username, pw)) {
            logger.warn(String.format("User %s with pw <%s> not in system", username, pw));
            proc_status = genErrorToClient("login failed");
        }

        // user can login:
        else {
            ud.login(connectionId, username);
            logger.warn(String.format("Client%d: User %s with pw <%s> logged in ", connectionId, username, pw));
            proc_status = genAckToClient("login succeeded");
        }

        AccessLimiter.finishRead(DB.USER);
        return proc_status;
    }


    /**
     * db_access:
     * Read: Users
     * Write: Users
     *
     * @param creds credentials to reg. new user
     * @return Fail-package:
     * > not enough args
     * > Client id already logged in
     * > Invalid data block
     * <p>
     * Success-package:
     * > all above are false
     */
    protected CmdPackage handleRegister(String[] creds) {
        CmdPackage proc_status = null;
        AccessLimiter.getWriteAccess(DB.USER);

        if (creds.length < 2) {
            logger.warn("registration failed because not enough arguments: " + Arrays.toString(creds));
            proc_status = genErrorToClient("registration failed");
        } else if (ud.isClientLogged(connectionId)) {
            logger.warn(String.format("Client%d already logged in!", connectionId));
            proc_status = genErrorToClient("registration failed");
        } else if (userInSystem(creds[0])) {
            logger.warn(String.format("User <%s> already registered", creds[0]));
            proc_status = genErrorToClient("registration failed");
        } else if (!validRegDataBlock(creds)) {
            logger.warn(String.format("Invalid Data block in: %s", Arrays.toString(creds)));
            proc_status = genErrorToClient("registration failed");
        } else {
            logger.info(String.format("Client%d registering user with credentials: %s",
                    connectionId, Arrays.toString(creds)));
            registerUser(creds);
            proc_status = genAckToClient("registration succeeded");
        }

        AccessLimiter.finishWrite(DB.USER);
        return proc_status;
    }


    /**
     * @param args arguments of request
     * @return echo nack the request - no processing for request done in basic protocol
     */
    protected CmdPackage handleRequest(String[] args) {
        return genAckToClient(String.format("CMD:<%s> ARGS:<%s>", MsgType.REQUEST.name(), Arrays.toString(args)));
    }


    /**
     * @param news Broadcast some message to all users
     */
    // todo rethink this broadcast design
    protected void broadcastSomeUberImportantNews(String news) {
        Set<Integer> userIds;

        // while broadcasting msg handling pauses
        synchronized (broadcastLock) {
            userIds = ud.getLoginStatuses().keySet();
        }

        for (Integer userId : userIds) {
            if (ud.isClientLogged(userId)) {
                connections.send(userId, "BROADCAST " + news);
            }
        }
    }


    /**
     * @param creds credentials during login
     * @return true - at this protocol every bloack is ok
     */
    protected boolean validRegDataBlock(String[] creds) {
        return true;
    }

    //------------------------------------------------------------------------------------------------------------------
    // ----------------------------------------------- DB Access -------------------------------------------------------

    protected void registerUser(String[] creds) {
        GenericUserDBInterface.addUser(creds[0], creds[1]);
    }

    protected boolean userInSystem(String username) {
        return GenericUserDBInterface.userExists(username);
    }

    protected boolean validUserPWcombo(String username, String pw) {
        return GenericUserDBInterface.userPWexists(username, pw);
    }

    //------------------------------------------------------------------------------------------------------------------
    // -------------------------------------------- answer pack generators ---------------------------------------------

    /**
     * @param err - error mesage
     * @return CmdPackage with error message
     */
    protected CmdPackage genErrorToClient(String err) {
        return genSomethingToClient(err, "ERROR");
    }


    /**
     * @param ack - Acknowledge message
     * @return CmdPackage with the message
     */
    protected CmdPackage genAckToClient(String ack) {
        return genSomethingToClient(ack, "ACK");
    }


    /**
     * @param msg     message to gen CmdPackage from
     * @param msgType - type of CmdPackage
     * @return CmdPackage with msg of type msgType
     */
    protected CmdPackage genSomethingToClient(String msg, String msgType) {
        try {
            return new CmdPackage(msgType + " " + msg, PackType.SERVER_RES);
        } catch (CmdParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean shouldTerminate() {
        // TODO: 1/3/18 complete method
        return false;
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BidiBasicUserProtocolImpl that = (BidiBasicUserProtocolImpl) o;
        return connectionId == that.connectionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionId);
    }
}



