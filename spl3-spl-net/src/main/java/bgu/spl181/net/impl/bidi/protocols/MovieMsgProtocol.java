package bgu.spl181.net.impl.bidi.protocols;

import bgu.spl181.net.dataholders.CmdPackage;
import bgu.spl181.net.dataholders.Movie;
import bgu.spl181.net.dataholders.MovieLender;
import bgu.spl181.net.dataholders.UsersData;
import bgu.spl181.net.json.AccessLimiter;
import bgu.spl181.net.json.MovieDBInterface;
import bgu.spl181.net.json.MovieLendersDBInterface;
import bgu.spl181.net.types.DB;
import bgu.spl181.net.types.PackType;
import bgu.spl181.net.types.ReqTypes;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class MovieMsgProtocol extends BidiBasicUserProtocolImpl {

    private String curUser;

    public MovieMsgProtocol(UsersData ud) {
        super(ud);
    }

    //------------------------------------------------------------------------------------------------------------------
    // ----------------------------------------------- Register helpers ------------------------------------------------

    /**
     * @param creds credentials during login
     * @return true iff data block consists of valid country info
     */
    @Override
    protected boolean validRegDataBlock(String[] creds) {

        if (creds.length < 3 || creds[2].split("=\"").length != 2) {
            return false;
        } else if (!creds[2].split("=\"")[0].equals("country") || creds[2].split("=\"")[1].length() <= 1) {
            return false;
        }

        return true;
    }

    //------------------------------------------------------------------------------------------------------------------
    // ----------------------------------------------- Handle Requests -------------------------------------------------

    /**
     * @param args arguments of request
     * @return processing status of request
     */
    @Override
    protected CmdPackage handleRequest(String[] args) {
        CmdPackage requestCP = genCmdPackage(String.join(" ", args), PackType.REQUEST);
        curUser = ud.getUserOfClient(connectionId);
        if (curUser == null || !ud.isClientLogged(connectionId)) {
            return requestFailed(requestCP.type.name().toLowerCase());
        }

        return handleRequestTask(requestCP);
    }


    /**
     * pass CmdPackage to the appropriate handler
     * further filtration done in handleAdminTask - if needed
     *
     * @param requestCP - CmdPackage with request
     * @return processing status of request
     */
    private CmdPackage handleRequestTask(CmdPackage requestCP) {
        CmdPackage answer = null;
        if (requestCP.type == ReqTypes.BALANCE) {
            answer = handleBalance(requestCP.args);
        } else if (requestCP.type == ReqTypes.INFO) {
            answer = handleInfo(requestCP.args);
        } else if (requestCP.type == ReqTypes.RENT) {
            answer = handleRent(parseName(requestCP.args[0]));
        } else if (requestCP.type == ReqTypes.RETURN) {
            answer = handleReturn(parseName(requestCP.args[0]));

        } else if (requestCP.type == ReqTypes.ADDMOVIE || requestCP.type == ReqTypes.REMMOVIE ||
                requestCP.type == ReqTypes.CHANGEPRICE) {

            answer = handleAdminTask(requestCP);
        }

        return answer;
    }


    /**
     * db_access:
     * Read: Users
     * Write: None
     * <p>
     * handle exclusively admin tasks - if found that user is not admin - return err pack
     *
     * @param adminTask - admin task only
     * @return answer package
     */
    private CmdPackage handleAdminTask(CmdPackage adminTask) {
        AccessLimiter.getReadAccess(DB.USER);

        if (!userIsAdmin(curUser)) {
            logger.warn(String.format("Request %s failed because user <%s> is not admin",
                    adminTask.type.name(), curUser));
            AccessLimiter.finishRead(DB.USER);
            return genErrorToClient(String.format("request %s failed", adminTask.type.name().toLowerCase()));
        }
        AccessLimiter.finishRead(DB.USER);

        // handle admin task with relevant handler after verification that actually is admin
        if (adminTask.type == ReqTypes.ADDMOVIE) {
            return handleAddMovie(adminTask.args);
        } else if (adminTask.type == ReqTypes.REMMOVIE) {
            return handleRemMovie(parseName(adminTask.args[0]));
        } else if (adminTask.type == ReqTypes.CHANGEPRICE) {
            return handleChangePrice(adminTask.args);
        }

        return null;
    }

    //------------------------------------------------------------------------------------------------------------------
    // -------------------------------------------- Request task handlers ----------------------------------------------

    /**
     * db_access:
     * Read: Users - in acquire info
     * Write: Users - in add money
     *
     * @param args
     * @return balance info / status on addition of balance
     */
    private CmdPackage handleBalance(String[] args) {

        CmdPackage proc_status = null;

        if (args.length == 1 && args[0].equals("info")) {
            AccessLimiter.getReadAccess(DB.USER);

            MovieLender user = getUser(curUser);
            logger.info(String.format("Returning balance <%s> of user: <%s>", user.getBalance(), curUser));
            proc_status = genAckToClient("balance " + user.getBalance());

            AccessLimiter.finishRead(DB.USER);

        } else if (args.length != 2 || !args[0].equals("add")) {
            return genErrorToClient("request balance failed");
        } else {
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                return genErrorToClient("request balance failed");
            }


            AccessLimiter.getWriteAccess(DB.USER);
            proc_status = addSomeCashToSomeMovieLover(curUser, amount);
            AccessLimiter.finishWrite(DB.USER);
        }

        return proc_status;
    }


    /**
     * db_access:
     * Read: Movies
     * Write: None
     *
     * @param args move name, or no name
     * @return no name -> all movie names ; name -> movie info
     */
    private CmdPackage handleInfo(String[] args) {
        CmdPackage proc_status = null;
        AccessLimiter.getReadAccess(DB.MOVIE);

        if (args.length == 0) {
            logger.info("Returning all movies to user!");
            proc_status = genAckToClient("info " + getAllMoviesNames());
        } else {
            proc_status = genMovieInfo(parseName(args[0]));
        }

        AccessLimiter.finishRead(DB.MOVIE);

        return proc_status;
    }


    /**
     * db_access:
     * Read: Movie & User
     * Write: Movie & User
     *
     * @param movieName movie name user wants to rent
     * @return Fail package:
     * > Movie not in system
     * > No availible copies of movie
     * > User doesn't have enough money for movie
     * > User is from country which is band for this movie
     * <p>
     * Success package:
     * > all above are false
     */
    private CmdPackage handleRent(String movieName) {
        Movie mv;
        MovieLender ml = MovieLendersDBInterface.getUser(curUser);
        CmdPackage proc_status = null;

        AccessLimiter.getReadAccess(DB.USER);
        if (ml.gotMovie(movieName)) {
            AccessLimiter.finishRead(DB.USER);
            return requestFailed("rent");
        }
        AccessLimiter.finishRead(DB.USER);

        AccessLimiter.getWriteAccess(DB.MOVIE);
        AccessLimiter.getWriteAccess(DB.USER);

        mv = MovieDBInterface.getMovie(movieName);

        if (mv == null) {
            logger.warn(String.format("Movie %s not in system!", movieName));
            proc_status = requestFailed("rent");
        } else if (mv.getAvailableAmount().equals("0")) {
            logger.warn(String.format("No available copies of movie %s!", movieName));
            proc_status = requestFailed("rent");
        } else if (Integer.parseInt(ml.getBalance()) < Integer.parseInt(mv.getPrice())) {
            logger.warn(String.format("No available MONEY for movie %s!", movieName));
            proc_status = requestFailed("rent");
        } else if (ml.fromOneOfCountries(mv.getBannedCountries())) {
            logger.warn(String.format("User is band for movie %s!", movieName));
            proc_status = requestFailed("rent");
        }
        else {
            mv.decreaseAvailability();
            MovieDBInterface.writeMovie(mv);

            broadcastMovieInfo(mv);
            ml.updateRentage(mv);
            MovieLendersDBInterface.writeMovieLender(ml);

            logger.info(String.format("Successfully rented movie %s", movieName));
            proc_status = genAckToClient(String.format("rent \"%s\" success", movieName));
        }

        AccessLimiter.finishWrite(DB.MOVIE);
        AccessLimiter.finishWrite(DB.USER);

        return proc_status;

    }


    /**
     * db_access:
     * Read: User
     * Write: Movie & User
     *
     * @param movieName - movie name to return
     * @return Fail-package:
     * > user didn't have the movie in first place
     * <p>
     * Success-package:
     * > otherwise
     */
    private CmdPackage handleReturn(String movieName) {
        CmdPackage proc_status = null;

        AccessLimiter.getReadAccess(DB.USER);
        MovieLender ml = MovieLendersDBInterface.getUser(curUser);

        if (!ml.gotMovie(movieName)) {
            logger.warn(String.format("User didn't have the movie <%s> in first place", movieName));
            AccessLimiter.finishRead(DB.USER);
            return requestFailed("return");
        }
        AccessLimiter.finishRead(DB.USER);

        AccessLimiter.getWriteAccess(DB.MOVIE);
        AccessLimiter.getWriteAccess(DB.USER);
        Movie mv = MovieDBInterface.getMovie(movieName);
        if (mv == null) {
            proc_status = requestFailed("return");
        } else {
            mv.increaseAvailability();
            MovieDBInterface.writeMovie(mv);

            broadcastMovieInfo(mv);

            ml.updateReturn(mv);
            MovieLendersDBInterface.writeMovieLender(ml);

            logger.warn(String.format("Successfully returned movie <%s>", movieName));
            proc_status = genAckToClient(String.format("return \"%s\" success", movieName));
        }

        AccessLimiter.finishWrite(DB.MOVIE);
        AccessLimiter.finishWrite(DB.USER);
        return proc_status;
    }

    //------------------------------------------------------------------------------------------------------------------
    // ------------------------------------------ AdminRequest task handlers -------------------------------------------

    /**
     * db_access:
     * Read: Movie
     * Write: Movie
     *
     * @param args movie name,amount,price,banned countries
     * @return Fail-package:
     * > price < 0
     * > Movie already in db
     * <p>
     * Success-package:
     * > otherwise
     */
    private CmdPackage handleAddMovie(String[] args) {

        CmdPackage proc_status = null;
        String movieName = parseName(args[0]);
        String amount = args[1];
        String price = args[2];
        ArrayList<String> bannedCountries = new ArrayList<>();

        // if banned countries exist add them to list above
        for (int i = 3; i < args.length; i++) {
            bannedCountries.add(parseName(args[i]));
        }

        if (Integer.parseInt(price) <= 0 || Integer.parseInt(amount) <= 0) {
            logger.warn("Cannot add movie with price/amount <= 0");
            return requestFailed("addmovie");
        }

        AccessLimiter.getWriteAccess(DB.MOVIE);
        AccessLimiter.getReadAccess(DB.USER);

        if (addMovieToDb(movieName, amount, price, bannedCountries)) {
            logger.info("Succeffully added movie: " + movieName);
            proc_status = genAckToClient(String.format("addmovie %s success", args[0]));
        } else {
            // movie already in system:
            logger.warn(String.format("Movie %s already in system", movieName));
            proc_status = requestFailed("addmovie");
        }

        AccessLimiter.finishWrite(DB.MOVIE);
        AccessLimiter.finishRead(DB.USER);
        return proc_status;
    }


    /**
     * db_acces:
     * Read: Users, Movies
     * Write: Movies
     *
     * @param movieName - movie to remove
     * @return Fail-package:
     * > Movie available amount != Movie initial amount
     * > Movie not in db
     * <p>
     * Success-package:
     * > otherwise
     */
    private CmdPackage handleRemMovie(String movieName) {

        // for logging purposes - return codes of remmovie in DB:
        // 0 - removed movie
        // 1 - movie doesn't exist
        // 2- some client rented the movie

        AccessLimiter.getWriteAccess(DB.MOVIE);
        AccessLimiter.getReadAccess(DB.USER);
        int res = tryRemMovieFromDB(movieName);
        AccessLimiter.finishWrite(DB.MOVIE);
        AccessLimiter.finishRead(DB.USER);

        switch (res) {
            case 0:
                logger.info("Removed movie: " + movieName);
                return genAckToClient(String.format("remmovie \"%s\" success", movieName));
            case 1:
                logger.warn(String.format("Cannot remove movie <%s> because doesn't exist in DB ", movieName));
                return requestFailed("remmovie");
            case 2:
                logger.warn(String.format("Cannot remove movie <%s> because some client is renting", movieName));
                return requestFailed("remmovie");
            default:
                return null;
        }
    }


    /**
     * db_access:
     * Read: Movie
     * Write: Movie
     *
     * @param args movie name & new price
     * @return Fail-package:
     * > new price <= 0
     * > Movie is not in DB
     * <p>
     * Success-package:
     * > otherwise
     */
    private CmdPackage handleChangePrice(String[] args) {
        String movieName = parseName(args[0]);
        String newPrice = args[1];

        if (Integer.parseInt(newPrice) <= 0) {
            logger.warn(String.format("New price for movie %s is <= 0 ", movieName));
            return requestFailed("changeprice");
        }

        // change price in DB + announce return codes
        // true - changed and broadcasted
        // false - didn't change because movie doesn't exist
        AccessLimiter.getWriteAccess(DB.MOVIE);
        boolean change_status = changePriceInDB(movieName, newPrice);
        AccessLimiter.finishWrite(DB.MOVIE);

        if (change_status) {
            logger.info(String.format("Change price of movie: %s to be: %s", movieName, newPrice));
            return genAckToClient(String.format("changeprice \"%s\" success", movieName));
        }

        logger.warn(String.format("Movie %s is not in DB ", movieName));
        return requestFailed("changeprice");
    }

    //------------------------------------------------------------------------------------------------------------------
    // ----------------------------------------------- DB Access -------------------------------------------------------

    @Override
    protected void registerUser(String[] creds) {
        MovieLendersDBInterface.addUser(creds[0], creds[1], creds[2]);
    }

    @Override
    protected boolean userInSystem(String username) {
        return MovieLendersDBInterface.userExists(username);
    }

    @Override
    protected boolean validUserPWcombo(String username, String pw) {
        return MovieLendersDBInterface.userPWexists(username, pw);
    }

    private boolean userIsAdmin(String userName) {
        return MovieLendersDBInterface.userIsAdmin(userName);
    }

    private MovieLender getUser(String userName) {
        return MovieLendersDBInterface.getUser(userName);
    }

    private CmdPackage addSomeCashToSomeMovieLover(String curUser, int amount) {
        String curBalance = MovieLendersDBInterface.addShmeckels(curUser, amount);
        if (!curBalance.equals("meh")) {
            logger.info(String.format("Adding balance amount: <%d> of user: <%s>", amount, curUser));
            return genAckToClient(String.format("balance %s added %d", curBalance, amount));
        }

        return genErrorToClient("request balance failed");
    }

    private String getAllMoviesNames() {
        List<Movie> movies = MovieDBInterface.getList();
        String names = "";
        if (movies.size() > 0) {
            for (Movie mv : movies) {
                names += "\"" + mv.getName() + "\" ";
            }
            names = names.substring(0, names.length() - 1);
        }

        return names;
    }

    private CmdPackage genMovieInfo(String movieName) {
        Movie mv = MovieDBInterface.getMovie(movieName);
        if (mv == null) {
            return genErrorToClient("request info failed");
        }

        logger.info("Generating info for movie: " + movieName);

        String movieInfo = String.format("info \"%s\" ", movieName);
        movieInfo += mv.getAvailableAmount() + " ";
        movieInfo += mv.getPrice();

        if (mv.getBannedCountries().length > 0) {
            for (String country : mv.getBannedCountries()) {
                movieInfo += String.format(" \"%s\"", country);
            }
        }

        return genAckToClient(movieInfo);
    }

    private boolean addMovieToDb(String movieName, String amount, String price, ArrayList<String> bannedCountries) {
        // movie id will be updated in database side - don't have enough info at this point
        Movie mv = new Movie("noId", movieName, price, amount, amount,
                bannedCountries.toArray(new String[bannedCountries.size()]));

        // close db while processing
        if (!MovieDBInterface.addMovie(mv)) {
            return false;
        }

        broadcastMovieInfo(mv);

        return true;
    }

    private int tryRemMovieFromDB(String movieName) {

        int res = MovieDBInterface.removeMovie(movieName);
        if (res == 0) {
            broadcastSomeUberImportantNews(String.format("movie \"%s\" removed", movieName));
        }

        return res;

    }

    private boolean changePriceInDB(String movieName, String newPrice) {

        String res = MovieDBInterface.changePrice(movieName, newPrice);
        if (!res.equals("meh")) {
            broadcastSomeUberImportantNews(String.format("movie \"%s\" %s %s", movieName, res, newPrice));
            return true;
        }

        return false;
    }

    //------------------------------------------------------------------------------------------------------------------
    // ------------------------------------------------- Service -------------------------------------------------------

    /**
     * @param nameWithQuotes string with quotes
     * @return string without quotes
     */
    private String parseName(String nameWithQuotes) {
        return StringUtils.substringsBetween(nameWithQuotes, "\"", "\"")[0];
    }


    /**
     * @param mv movie name
     *           Broadcast movie info to all users
     */
    private void broadcastMovieInfo(Movie mv) {
        broadcastSomeUberImportantNews(
                String.format("movie \"%s\" %s %s", mv.getName(), mv.getAvailableAmount(), mv.getPrice()));
    }


    /**
     * @param req name of request
     * @return fail CmdPackage with according request type message
     */
    private CmdPackage requestFailed(String req) {
        return genErrorToClient("request " + req + " failed");
    }
}
