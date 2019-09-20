

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.cache.Cache;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.htmlparser.jericho.Source;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

@SuppressWarnings("serial")
public class TweetServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(TweetServlet.class
			.getName());
	private static final String[] space = { "　", "　", " ", " " };
	private static Twitter twitter = new TwitterFactory().getInstance();
	private String wikiStatus;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		Random rnd = new Random();

		int type = rnd.nextInt(4);

		String tweet = getPhrase(type);

		if (tweet == null) {
			// if (true) {
			return;
		}

		// ツイート
		try {
			Status status = twitter.updateStatus("わぁい" + tweet + space[type]
					+ "あかり" + tweet + "大好き");
			log.info("Successfully updated the status to [" + status.getText()
					+ "].");
			if (type <= 1) {
				tweetAkaripedia();
			}
		} catch (TwitterException e) {
			e.printStackTrace();
			log.warning("Failed updated the status " + e.getErrorMessage());
		}
	}

	// 0-1,Wikipedia / 2.キーフレーズの読み込み / 3.形態素解析結果の読み込み
	@SuppressWarnings("unchecked")
	public String getPhrase(int type) {
		String phrase = null;
		Cache cache = AkariUtil.getCache();

		if (type <= 1) {
			try {
				URL url = new URL(
						"https://ja.wikipedia.org/wiki/Special:Randompage");

				HttpURLConnection http = (HttpURLConnection) url
						.openConnection();
				http.setRequestProperty("User-Agent",
						"Akkarin/1.0 (compatible; AkariDaisukiBot/1.0)");

				Source source = new Source(http.getInputStream());
				net.htmlparser.jericho.Element el = source
						.getElementById("firstHeading");

				phrase = el.getTextExtractor().toString();
				log.info("Picked phrase from Wikipedia [" + phrase + "].");

				// カテゴリでフィルタリング、該当したら再帰でもう一度タイトル取得
				try {
					List<net.htmlparser.jericho.Element> catEls = source
							.getElementById("mw-normal-catlinks")
							.getAllElements("a");
					for (net.htmlparser.jericho.Element cat : catEls) {
						if (cat.getTextExtractor()
								.toString()
								.matches(
										".*(事件|犯罪|虐待|殺|暴力団|事故|災害|地震|津波|火災|障害|疾患|病|症候|がん|差別|テロ).*")) {
							log.info("Category is filtering.");
							return (getPhrase(0));
						}
					}
				} catch (Exception e) {
					log.info("Category not found.");
				}

				String wikiUrl = http.getURL().toString();
				String wikiContent = null;

				// akari_source用記事抽出コード
				try {
					List<net.htmlparser.jericho.Element> contentEls = source
							.getFirstElementByClass("mw-content-ltr")
							.getAllElements("p");

					for (net.htmlparser.jericho.Element content : contentEls) {
						if (content.getTextExtractor().toString()
								.matches(".*。.*")) {
							wikiContent = content.getTextExtractor().toString();
							break;
						}
					}

					Pattern p = Pattern.compile(".+?。");
					Matcher m = p.matcher(wikiContent);
					if (m.find()) {
						wikiContent = m.group(0).replaceAll("\\[.*\\]", "");
						if (wikiContent.length() >= 117) {
							wikiContent = wikiContent.substring(0, 115) + "…";
						}
					}
					if (wikiContent == null) {
						wikiContent = phrase;
					}
				} catch (Exception e) {
					wikiContent = phrase;
				}

				wikiStatus = wikiContent + " " + wikiUrl;
			} catch (Exception e) {
				e.printStackTrace();
				log.severe(e.getLocalizedMessage());
			}
		} else if (type == 2) {
			Random rnd = new Random();
			StringBuilder sb = new StringBuilder();
			List<String> keyPhraseList = new ArrayList<String>();

			// メモリキャッシュからkeyPhraseListを取得
			if (cache.get("keyPhraseList") != null) {
				keyPhraseList = (List<String>) cache.get("keyPhraseList");
			} else {
				log.info("phraseList not cached.");
				return null;
			}

			sb.append("Key phrase : ");
			for (String keyPhrase : keyPhraseList) {
				sb.append(keyPhrase + ", ");
			}

			log.info(sb.toString());

			phrase = keyPhraseList.get(rnd.nextInt(keyPhraseList.size()));

			log.info("Picked key phrase from TimeLine [" + phrase + "].");
		} else if (type == 3) {
			Random rnd = new Random();
			StringBuilder sb = new StringBuilder();
			List<String> MAList = new ArrayList<String>();

			// メモリキャッシュからMAListを取得
			if (cache.get("MAList") != null) {
				MAList = (List<String>) cache.get("MAList");
			} else {
				log.info("MAList not cached.");
				return null;
			}

			sb.append("MA phrase : ");
			for (String MAPhrase : MAList) {
				sb.append(MAPhrase + ", ");
			}

			log.info(sb.toString());

			phrase = MAList.get(rnd.nextInt(MAList.size()));

			log.info("Picked MA phrase from TimeLine [" + phrase + "].");
		}

		if (phrase != null) {
			if (phrase.matches(".+(事件|殺|事故|津波|火災|震災).*")) {
				return getPhrase(type);
			} else {
				cache.remove("MAList");
				cache.remove("keyPhraseList");
			}
		}

		return phrase;
	}

	// akaripediaでWikipediaのリンクをツイート
	private void tweetAkaripedia() {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(false)
				.setOAuthConsumerKey("YOUR_CONSUMER_KEY_HERE")
				.setOAuthConsumerSecret(
						"YOUR_CONSUMER_SECRET_HERE")
				.setOAuthAccessToken(
						"YOUR_TOKEN_HERE")
				.setOAuthAccessTokenSecret(
						"YOUR_TOKEN_SECRET_HERE");
		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();

		try {
			Status status = twitter.updateStatus(wikiStatus);
			log.info("Successfully updated the akaripedia status to ["
					+ status.getText() + "].");
		} catch (TwitterException e) {
			e.printStackTrace();
			log.warning("Failed updated the status " + e.getErrorMessage());
		}
	}
}
