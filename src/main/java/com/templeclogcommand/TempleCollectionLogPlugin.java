package net.runelite.client.plugins.templecollectionlog;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatClient;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ChatInput;
import net.runelite.client.game.ItemManager;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.chatcommands.ChatCommandsPlugin;
import net.runelite.client.plugins.chatcommands.ChatKeyboardListener;
import net.runelite.client.plugins.templecollectionlog.TempleCollectionLogConfig;
import net.runelite.client.plugins.templecollectionlog.TempleOsrsClient;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import org.apache.commons.text.WordUtils;

@PluginDescriptor(
        name = "Temple Collection Log",
        description = "Enable Temple Collection Log",
        tags = {"log"}
)

@PluginDependency(ChatCommandsPlugin.class)
public class TempleCollectionLogPlugin extends Plugin
{
    @Inject
    private KeyManager keyManager;
    @Inject
    private ChatCommandManager chatCommandManager;
    @Inject
    private ChatKeyboardListener chatKeyboardListener;
    @Inject
    private ClientThread clientThread;
    @Inject
    private Client client;
    @Inject
    private net.runelite.client.plugins.templecollectionlog.TempleCollectionLogConfig config;
    @Inject
    private ItemManager itemManager;
    @Inject
    private ConfigManager configManager;
    @Inject
    private Gson gson;
    @Inject
    private TempleOsrsClient templeOsrsClient;
    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private HiscoreClient hiscoreClient;

    @Inject
    private RuneLiteConfig runeLiteConfig;

    private static final int CLOG_MAGIC_CONSTANT = 1000000;
    private static final String COLLECTION_LOG_COMMAND_STRING = "!log";
    private int collectionLogIconsIdx = -1;
    static final int COL_LOG_ENTRY_HEADER_TITLE_INDEX = 0;

    @Override
    public void startUp()
    {
        keyManager.registerKeyListener(chatKeyboardListener);

        chatCommandManager.registerCommandAsync(COLLECTION_LOG_COMMAND_STRING, this::clogLookup);


        clientThread.invoke(() ->
        {
            if (client.getGameState().getState() >= GameState.LOGIN_SCREEN.getState())
            {

            }
        });
    }
    @Override
    public void shutDown()
    {

        keyManager.unregisterKeyListener(chatKeyboardListener);

        chatCommandManager.unregisterCommand(COLLECTION_LOG_COMMAND_STRING);
    }

    @Provides
    net.runelite.client.plugins.templecollectionlog.TempleCollectionLogConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TempleCollectionLogConfig.class);
    }

    @VisibleForTesting
    void clogLookup(ChatMessage chatMessage, String message)
    {
        if ( !config.log() )
        {
            return;
        }

        String search;

        if (message.length() <= COLLECTION_LOG_COMMAND_STRING.length())
        {
            return;
        }

        search = message.substring(COLLECTION_LOG_COMMAND_STRING.length() + 1);

        search = parseCollectionLogBossName( search );
        String searchApi;
        if ( search != "Rogue's Den" )
        {
            searchApi = search.replace( " ", "_" ).replace("'", "_").toLowerCase();
        }
        else
        {
            searchApi = "rogues_den";
        }

        if ( searchApi.equals("bosses") && !config.bosses() )
        {
            return;
        }
        if ( searchApi.equals("minigames") && !config.minigames() )
        {
            return;
        }
        if ( searchApi.equals("other") && !config.other() )
        {
            return;
        }
        if ( searchApi.equals("clues") && !config.clues() )
        {
            return;
        }
        if ( searchApi.equals("all") && !config.all() )
        {
            return;
        }

        List<Integer> playerClogList = new ArrayList<>();
        ChatMessageType type = chatMessage.getType();

        final String player;
        if (type.equals(ChatMessageType.PRIVATECHATOUT))
        {
            player = client.getLocalPlayer().getName();
        }
        else
        {
            player = Text.sanitize(chatMessage.getName());
        }

        try
        {
//            playerClogList = templeOsrsClient.getCollectionLogList( player, search );
            String str = templeOsrsClient.getCollectionLogList( player, searchApi );
            int searchIdx;
            if ( !searchApi.equals("bosses") &&
                 !searchApi.equals("all") &&
                 !searchApi.equals("raids") &&
                 !searchApi.equals("clues") &&
                 !searchApi.equals("minigames") &&
                 !searchApi.equals("other") &&
                 !searchApi.equals("gilded") &&
                 !searchApi.equals("mimic") &&
                 !searchApi.equals("third_age") )

            {
              searchIdx = str.indexOf( "\"" + searchApi + "\":" );
            }
            else
            {
                searchIdx = str.indexOf( "\"items\":" );
            }

            int altSearchIdx = -1;
            int searchIdx3 = -1;

            if ( searchIdx == -1 )
            {
                String response = "No TempleOSRS Clog data found for " + search;
                final MessageNode messageNode = chatMessage.getMessageNode();
                messageNode.setValue(response);
                client.refreshChat();
            }

            while ( searchIdx != -1 )
            {
                searchIdx = str.indexOf( "\"id\":", searchIdx );
                searchIdx3 = -1;
                altSearchIdx = -1;
                if ( searchIdx != -1 )
                {
                    altSearchIdx = str.indexOf(",\"count\":", searchIdx );
                }

                if ( altSearchIdx != -1 )
                {
                    searchIdx3 = str.indexOf(",\"date\":", altSearchIdx );

                    if ( searchIdx3 != -1 )
                    {
                        playerClogList.add( Integer.parseInt( str.substring(altSearchIdx + 9, searchIdx3 ) ) );
                        playerClogList.add( Integer.parseInt( str.substring( searchIdx + 5, altSearchIdx ) ) );
                    }
                }
                searchIdx = searchIdx3;
            }
        }
        catch ( Exception ex )
        {
            String response = "No TempleOSRS Clog Data found for user";
            final MessageNode messageNode = chatMessage.getMessageNode();
            messageNode.setValue(response);
            client.refreshChat();
        }

        if ( !playerClogList.isEmpty() )
        {
            loadCollectionLogIcons( playerClogList );

            ChatMessageBuilder responseBuilder = new ChatMessageBuilder()
                    .append(ChatColorType.NORMAL)
                    .append( search )
                    .append(": (" + ( playerClogList.size()  / 2 )+ ")");

            // Append pets that the player owns

            int imgIdx = 0;
            for (int clogIdx = 1; clogIdx < playerClogList.size(); clogIdx += 2)
            {
                responseBuilder.append( " " + playerClogList.get( clogIdx - 1 ).toString() + "x" ).img( collectionLogIconsIdx + imgIdx );
                imgIdx++;
            }
            String response = responseBuilder.build();

            final MessageNode messageNode = chatMessage.getMessageNode();
            messageNode.setRuneLiteFormatMessage(response);
            client.refreshChat();
        }
    }

    String parseCollectionLogBossName( String searchParam )
    {

        String lowerSearch = searchParam.toLowerCase();
        String fullBossName;

        switch (lowerSearch)
        {
            case "abby sire":
            case "sire":
                fullBossName = "Abyssal Sire";
                break;
            case "hydra":
                fullBossName =  "Alchemical Hydra";
                break;
            case "amox":
                fullBossName = "Amoxliatl";
                break;
            case "arax":
                fullBossName = "Araxxor";
                break;
            case "barrows":
                fullBossName = "Barrows Chests";
                break;
            case "bryo":
                fullBossName = "Bryophyta";
                break;
            case "callisto":
            case "artio":
                fullBossName = "Callisto and Artio";
                break;
            case "cerb":
                fullBossName = "Cerberus";
                break;
            case "chaos ele":
                fullBossName = "Chaos Elemental";
                break;
            case "chaos fan":
                fullBossName = "Chaos Fanatic";
                break;
            case "sara":
            case "saradomin":
            case "zilyana":
            case "zily":
                fullBossName = "Commander Zilyana";
                break;
            case "corp beast":
            case "corporeal beast":
            case "corp":
                fullBossName = "Corporeal Beast";
                break;
            case "crazy arch":
                fullBossName = "Crazy Archaeologist";
                break;
            case "supreme":
            case "rex":
            case "prime":
            case "dks":
                fullBossName = "Dagannoth Kings";
                break;
            case "duke":
                fullBossName = "Duke Sucellus";
                break;
            case "fight caves":
            case "the fight caves":
            case "jad":
            case "tzhaar fight cave":
                fullBossName = "The Fight Caves";
                break;
            case "sol heredit":
            case "sol":
            case "colo":
            case "colosseum":
            case "fortis colosseum":
                fullBossName = "Fortis Colosseum";
                break;
            case "cgaunt":
            case "cgauntlet":
            case "the corrupted gauntlet":
            case "cg":
            case "gaunt":
            case "gauntlet":
            case "the gauntlet":
                fullBossName = "The Gauntlet";
                break;
            case "bando":
            case "bandos":
            case "graardor":
                fullBossName = "General Graardor";
                break;
            case "mole":
                fullBossName = "Giant Mole";
                break;
            case "dusk":
            case "dawn":
            case "gargs":
            case "ggs":
            case "gg":
                fullBossName = "Grotesque Guardians";
                break;
            case "huey":
            case "hueycoatl":
                fullBossName = "The Hueycoatl";
                break;
            case "zuk":
            case "inferno":
                fullBossName = "The Inferno";
                break;
            case "kq":
                fullBossName = "Kalphite Queen";
                break;
            case "kbd":
                fullBossName = "King Black Dragon";
                break;
            case "arma":
            case "kree":
            case "kreearra":
            case "armadyl":
                fullBossName = "Kree'arra";
                break;
            case "zammy":
            case "zamorak":
            case "kril":
            case "kril tsutsaroth":
                fullBossName = "K'ril Tsutsaroth";
                break;
            case "leviathan":
            case "levi":
                fullBossName = "The Leviathan";
                break;
            case "moons":
            case "lunar chests":
            case "perilous moons":
            case "perilous moon":
                fullBossName = "Moons of Peril";
                break;
            case "nightmare":
            case "nm":
            case "tnm":
            case "nmare":
            case "pnm":
            case "phosani":
            case "phosanis":
            case "phosani nm":
            case "phosani nightmare":
            case "phosanis nightmare":
                fullBossName = "The Nightmare";
                break;
            case "phantom":
            case "muspah":
            case "pm":
                fullBossName = "Phantom Muspah";
                break;
            case "titans":
            case "rt":
                fullBossName = "Royal Titans";
                break;
            case "sarach":
                fullBossName = "Sarachnis";
                break;
            case "scorp":
                fullBossName = "Scorpia";
                break;
            case "scurry":
                fullBossName = "Scurrius";
                break;
            case "temp":
            case "fishingtodt":
            case "fishtodt":
                fullBossName = "Tempoross";
                break;
            case "smoke devil":
            case "thermy":
                fullBossName = "Thermonuclear Smoke Devil";
                break;
            case "vard":
                fullBossName = "Vardorvis";
                break;
            case "spindel":
            case "venenatis":
            case "vene":
                fullBossName = "Venenatis and Spindel";
                break;
            case "vetion":
            case "calvarion":
            case "calv":
                fullBossName = "Vetion and Calvarion";
                break;
            case "vork":
                fullBossName = "Vorkath";
                break;
            case "wisperer":
            case "whisperer":
            case "whisp":
            case "wisp":
                fullBossName = "The Whisperer";
                break;
            case "wt":
                fullBossName = "Wintertodt";
                break;
            case "zalc":
                fullBossName = "Zalcano";
                break;
            case "cm cox":
            case "cox cm":
            case "cox":
            case "xeric":
            case "chambers":
            case "olm":
                fullBossName = "Chambers of Xeric";
                break;
            case "hmt":
            case "tob":
            case "theatre":
            case "verzik":
            case "verzik vitur":
                fullBossName = "Theatre of Blood";
                break;
            case "toa":
            case "tombs":
            case "amascut":
            case "warden":
            case "wardens":
                fullBossName = "Tombs of Amascut";
                break;
            case "beginner":
            case "beginners":
            case "clues beginner":
            case "beginner clues":
                fullBossName = "Beginner Treasure Trails";
                break;
            case "easy":
            case "easies":
            case "easys":
            case "clues easy":
            case "easy clues":
                fullBossName = "Easy Treasure Trails";
                break;
            case "medium":
            case "clues medium":
            case "medium clues":
            case "mediums":
                fullBossName = "Medium Treasure Trails";
                break;
            case "hard":
            case "clues hard":
            case "hard clues":
            case "hards":
                fullBossName = "Hard Treasure Trails";
                break;
            case "elite":
            case "clues elite":
            case "elite clues":
            case "elites":
                fullBossName = "Elite Treasure Trails";
                break;
            case "master":
            case "clues master":
            case "master clues":
            case "masters":
                fullBossName = "Master Treasure Trails";
                break;
//            Unused by temple api
//            case "clues hard rare":
//            case "hard clues rare":
//            case "hard rare":
//            case "hard rares":
//            case "hard treasure trails rare":
//                fullBossName = "Hard Treasure Trails (Rare)";
//                break;
//            Unused by temple api
//            case "clues elite rare":
//            case "elite clues rare":
//            case "elite rare":
//            case "elite rares":
//            case "elite treasure trails rare":
//                fullBossName = "Elite Treasure Trails (Rare)";
//                break;
// Unused by temple api
//            case "clues master rare":
//            case "master clues rare":
//            case "master rare":
//            case "master rares":
//            case "master treasure trails rare":
//                fullBossName = "Master Treasure Trails (Rare)";
//                break;
            case "clues shared":
            case "shared clues":
                fullBossName = "Shared Treasure Trail Rewards";
                break;
            case "ba":
            case "barb assault":
                fullBossName = "Barbarian Assault";
                break;
            case "brim":
            case "brimhaven":
            case "brimhaven agility":
                fullBossName = "Brimhaven Agility Arena";
                break;
            case "cwars":
            case "cw":
                fullBossName = "Castle Wars";
                break;
            case "trawler":
                fullBossName = "Fishing Trawler";
                break;
            case "gf":
            case "foundry":
            case "giants foundry":
                fullBossName = "Giant's Foundry";
                break;
            case "gnome":
            case "restaurant":
            case "gnome rest":
                fullBossName = "Gnome Restaurant";
                break;
            case "gotr":
            case "runetodt":
            case "rifts closed":
                fullBossName = "Guardians of the Rift";
                break;
            case "sep":
            case "seppy":
            case "hs":
            case "sepulchre":
            case "ghc":
                fullBossName = "Hallowed Sepulchre";
                break;
            case "lms":
                fullBossName = "Last Man Standing";
                break;
            case "mta":
            case "mage training arena":
                fullBossName = "Magic Training Arena";
                break;
            case "mh":
            case "mahog homes":
                fullBossName = "Mahogany Homes";
                break;
            case "mixology":
            case "mm":
            case "master mixology":
            case "master mix":
            case "mastering mix":
                fullBossName = "Mastering Mixology";
                break;
            case "pc":
            case "pest":
            case "pest ctrl":
                fullBossName = "Pest Control";
                break;
            case "rogues":
            case "rogues den":
                fullBossName = "Rogues Den";
                break;
            case "shades":
            case "morton":
            case "mortton":
            case "shades of morton":
            case "shades of mortton":
                fullBossName = "Shades of Mortton";
                break;
            case "sw":
            case "zeal":
                fullBossName = "Soul Wars";
                break;
            case "trek":
            case "trekking":
                fullBossName = "Temple Trekking";
                break;
            case "tithe":
                fullBossName = "Tithe Farm";
                break;
            case "tb":
            case "brewing":
            case "trouble brew":
                fullBossName = "Trouble Brewing";
                break;
            case "vm":
            case "volcanic":
                fullBossName = "Volcanic Mine";
                break;
            case "aerial":
                fullBossName = "Aerial Fishing";
                break;
            case "pets":
                fullBossName = "All Pets";
                break;
            case "cam":
                fullBossName = "Camdozaal";
                break;
            case "scrolls":
            case "champion scrolls":
            case "champions challenge":
                fullBossName = "Champions Challenge";
                break;
            case "chompy":
            case "chompies":
            case "chompy birds":
                fullBossName = "Chompy Bird Hunting";
                break;
            // Colossal Wyrm Basic Agility Course
            case "wyrm course":
            case "wbac":
            case "cwbac":
            case "wyrmb":
            case "wyrmbasic":
            case "wyrm basic":
            case "colossal basic":
            case "colossal wyrm basic":
            case "colossal wyrm advanced":
                fullBossName = "Colossal Wyrm Agility";
                break;
            case "defenders":
            case "cyclops":
                fullBossName = "Cyclopes";
                break;
            case "chaos druids":
                fullBossName = "Elder Chaos Druids";
                break;
            case "fossil island":
                fullBossName = "Fossil Island Notes";
                break;
            case "demonics":
            case "demonic gorillas":
            case "gloughs experiments":
            case "zenytes":
            case "zennys":
                fullBossName = "Gloughs Experiments";
                break;
            case "hunters guild":
            case "hunter guild":
            case "hunterrumour":
            case "hunter contract":
            case "hunter contracts":
            case "hunter tasks":
            case "hunter task":
            case "hunter rumours":
            case "hunter rumors":
            case "rumors":
            case "rumor":
            case "rumours":
            case "rumour":
                fullBossName = "Hunter Guild";
                break;
            case "aa":
            case "ape atoll":
            case "ape atoll agility course":
            case "ape atoll agi":
            case "ape atoll agility":
                fullBossName = "Monkey Backpacks";
                break;
            case "mlm":
                fullBossName = "Motherlode Mine";
                break;
            case "notes":
                fullBossName = "My Notes";
                break;
            case "randoms":
            case "random":
                fullBossName = "Random Events";
                break;
            case "revs":
            case "rev":
                fullBossName = "Revenants";
                break;
            case "rooftops":
            case "rooftop":
            case "graceful":
            case "mogs":
                fullBossName = "Rooftop Agility";
                break;
            case "shayzien":
                fullBossName = "Shayzien Armour";
                break;
            case "stars":
                fullBossName = "Shooting Stars";
                break;
            case "pets skilling":
            case "skill pets":
                fullBossName = "Skilling Pets";
                break;
            case "tds":
                fullBossName = "Tormented Demons";
                break;
            case "misc":
                fullBossName = "Miscellaneous";
                break;
            default:
                fullBossName = WordUtils.capitalize(searchParam);
                break;
        }

        return fullBossName;
    }
    /**
     * Loads the sprites of a set of item ids within the collection log list to be able to display the images
     *
     * @param clogList   Full collection log list from json, not all entries will be converted to images
     */
    private void loadCollectionLogIcons( List<Integer> clogList )
    {
        final IndexedSprite[] modIcons = client.getModIcons();
        assert modIcons != null;

        final IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, modIcons.length + clogList.size());
        collectionLogIconsIdx = modIcons.length;

        client.setModIcons(newModIcons);
        int imgIdx = 0;

        if ( clogList.size() > 0 )
        {
            for (int i = 1; i < clogList.size(); i += 2)
            {
                final int clogID = clogList.get(i);

                final AsyncBufferedImage abi = itemManager.getImage(clogID);
                final int idx = collectionLogIconsIdx + imgIdx;
                abi.onLoaded(() ->
                {
                    final BufferedImage image = ImageUtil.resizeImage(abi, 18, 16);
                    final IndexedSprite sprite = ImageUtil.getImageIndexedSprite(image, client);
                    // modicons array might be replaced in between when we assign it and the callback,
                    // so fetch modicons again
                    client.getModIcons()[idx] = sprite;
                });

                imgIdx++;
            }
        }

    }

}
