package commands.classic;

import commands.model.DofusRequestCommand;
import enums.Language;
import exceptions.*;
import sx.blah.discord.handle.obj.Permissions;
import util.*;
import data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.obj.IMessage;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.regex.Matcher;

/**
 * Created by steve on 12/10/2016.
 */
public class SetCommand extends DofusRequestCommand {

    private final static Logger LOG = LoggerFactory.getLogger(SetCommand.class);
    private final static String forName = "text=";
    private DiscordException tooMuchSets;
    private DiscordException notFoundSet;
    private DiscordException noExternalEmoji;

    public SetCommand(){
        super("set", "\\s+(-more)?(.*)");
        tooMuchSets = new TooMuchDiscordException("exception.toomuch.sets", "exception.toomuch.sets_found");
        notFoundSet = new NotFoundDiscordException("exception.notfound.set", "exception.notfound.set_found");
        noExternalEmoji = new BasicDiscordException("exception.basic.no_external_emoji_permission");
    }

    @Override
    public boolean request(IMessage message) {
        if (super.request(message)) {
            Matcher m = getMatcher(message);
            Language lg = Translator.getLanguageFrom(message.getChannel());
            m.find();

            if (message.getChannel().getModifiedPermissions(ClientConfig.DISCORD().getOurUser()).contains(Permissions.USE_EXTERNAL_EMOJIS)
                    && ClientConfig.DISCORD().getOurUser().getPermissionsForGuild(message.getGuild())
                    .contains(Permissions.USE_EXTERNAL_EMOJIS)) {
                String normalName = Normalizer.normalize(m.group(2).trim(), Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
                String editedName = removeUselessWords(lg, normalName);
                BestMatcher matcher = new BestMatcher(normalName);

                try {
                    matcher.evaluateAll(getListRequestableFrom(getSearchURL(lg, editedName), message, notFoundSet));

                    if (matcher.isUnique()) { // We have found it !
                        Embedded set = Set.getSet(lg, Translator.getLabel(lg, "game.url")
                                + matcher.getBest().getUrl());
                        if (m.group(1) != null)
                            Message.sendEmbed(message.getChannel(), set.getMoreEmbedObject(lg));
                        else
                            Message.sendEmbed(message.getChannel(), set.getEmbedObject(lg));
                    } else if (!matcher.isEmpty())  // Too much sets
                        tooMuchSets.throwException(message, this, lg, matcher.getBests());
                    else // empty
                        notFoundSet.throwException(message, this, lg);
                } catch (IOException e) {
                    ExceptionManager.manageIOException(e, message, this, lg, notFoundSet);
                }

                return true;
            }
            else
                noExternalEmoji.throwException(message, this, lg);
        }

        return false;
    }

    private String getSearchURL(Language lg, String text) throws UnsupportedEncodingException {
        return Translator.getLabel(lg, "game.url") + Translator.getLabel(lg, "set.url")
                + "?" + forName.toLowerCase() + URLEncoder.encode(text, "UTF-8")
                + "&" + forName.toUpperCase() + URLEncoder.encode(text, "UTF-8");
    }

    private String removeUselessWords(Language lg, String search){
        return search.replaceAll("\\s+\\w{1,2}\\s+", " ")
                .replaceAll(Translator.getLabel(lg, "set.meta"), "");
    }

    @Override
    public String help(Language lg, String prefixe) {
        return "**" + prefixe + name + "** " + Translator.getLabel(lg, "set.help");
    }

    @Override
    public String helpDetailed(Language lg, String prefixe) {
        return help(lg, prefixe)
                + "\n" + prefixe + "`"  + name + " `*`set`* : " + Translator.getLabel(lg, "set.help.detailed.1")
                + "\n" + prefixe + "`"  + name + " -more `*`set`* : " + Translator.getLabel(lg, "set.help.detailed.2") + "\n";
    }
}
