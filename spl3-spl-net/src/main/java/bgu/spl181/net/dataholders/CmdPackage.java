package bgu.spl181.net.dataholders;

import bgu.spl181.net.exceptions.CmdParseException;
import bgu.spl181.net.types.CmdTypes;
import bgu.spl181.net.types.MsgType;
import bgu.spl181.net.types.PackType;
import bgu.spl181.net.srv.BaseServer;
import bgu.spl181.net.types.ReqTypes;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CmdPackage {

    public final Enum type;
    public final String[] args;
    private static Logger logger = BaseServer.logger;


    public CmdPackage(String msg, PackType pt) throws CmdParseException {
        ImmutablePair<Enum, String[]> typeMsg = genTypeMsg(msg, pt);
        this.type = typeMsg.left;
        this.args = typeMsg.right;
    }

    private ImmutablePair<Enum, String[]> genTypeMsg(String msg, PackType pt) throws CmdParseException {
        int firstSpaceIndx = msg.indexOf(' ');
        String msgPart;
        String argPart[];

        // arg-less cmd
        if (firstSpaceIndx == -1) {
            if (msg.length() >= 3) {
                msgPart = msg;
                argPart = new String[]{};
            }
            else {
                throw new CmdParseException(
                        String.format("Cmd string <%s> doesn't match protocol", msg));
            }
        }

        else {
            msgPart = msg.substring(0, firstSpaceIndx);

            ArrayList<String> l = new ArrayList<String>();
            Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(msg.substring(firstSpaceIndx + 1));
            while (m.find())
                l.add(m.group(1)); // Add .replace("\"", "") to remove surrounding quotes.

            argPart = new String[l.size()];
            l.toArray(argPart);
        }

        Enum type = decideEnum(msgPart, pt);
        logger.info(String.format("Parsed type: <%s> and args: <%s>", type.name(), Arrays.toString(argPart)));
        return new ImmutablePair<>(type, argPart);
    }

    private Enum decideEnum(String msgPart, PackType pt) throws CmdParseException {
        try {
            if (pt == PackType.CLIENT_MSG) {
                return MsgType.valueOf(msgPart);
            } else if (pt == PackType.REQUEST) {
              return ReqTypes.valueOf(msgPart.toUpperCase());
            } else {
                return CmdTypes.valueOf(msgPart);
            }
        }
        catch (IllegalArgumentException ex) {
            throw new CmdParseException(
                    String.format("Couldn't find cmd type: <%s> in packType: %s", msgPart, pt.name()));
        }
    }

    public static String assembleMsg(CmdPackage pack) {
        String msg = pack.type.name();

        if (pack.args.length >0){
            msg += " " + String.join(" ", pack.args);
        }

        logger.info("Assembled msg: " + msg);
        return msg;
    }


}
