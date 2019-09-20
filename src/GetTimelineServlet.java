

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.cache.Cache;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

@SuppressWarnings("serial")
public class GetTimelineServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(GetTimelineServlet.class
			.getName());
	private static final String blackList = "twittbot\\.net|soen\\.do|tweepieJP|Twibow|tw-b\\.com|BotMaker|twiroboJP|rakubo2|twisuke\\.com|twicastard|youbird|"
			+ "foursquare|gohantabeyo\\.com|Intel Tweet City|Tweet Button|twitterfeed|Ustream\\.TV|ニコニコ動画|ニコニコ生放送";

	// TLを取得、キーフレーズ抽出の結果をkeyPhraseListに格納
	@SuppressWarnings("unchecked")
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		Twitter twitter = new TwitterFactory().getInstance();

		try {
			long sinceID = 1;
			Cache cache = AkariUtil.getCache();

			// キャッシュからsinceIDを取得
			if (cache.get("sinceID") != null) {
				sinceID = (Long) cache.get("sinceID");
			}

			// home_timeline取得、timelineに代入
			String timeline = "";
			StringBuilder sb = new StringBuilder();
			Paging paging = new Paging(1, 150, sinceID);

			List<Status> statuses;

			statuses = twitter.getHomeTimeline(paging);

			for (Status status : statuses) {
				if (!status.getUser().getScreenName().equals("akari_daisuki")
						&& !status.getUser().isProtected()
						&& !status.isRetweet()
						&& !status.getSource().matches(
								".*(" + blackList + ").*")) {
					sb.append(status.getText() + "\n");
				}
			}

			timeline = sb.toString();

			// 不要な文字列を消去
			timeline = timeline
					.replaceAll(
							"((RT|QT|ＲＴ|ＱＴ)[ :]*(@|＠)[_A-Za-z0-9]+[ :]*|"
									+ "(#|＃)[_A-Za-z0-9]*|(@|＠)[_A-Za-z0-9]+|"
									+ "https?://t\\.co/[A-Za-z0-9]+|"
									+ "(ｗｗ|ww|ＷＷ|WW)[ｗwＷW]*|"
									+ "(I|Ｉ)(d|ｄ|D|Ｄ)[^-_.,A-Za-z0-9]+[-_.,A-Za-z0-9]+)",
							"");

			// log.info(timeline);

			// TODO tsukasaBot オノマトペ？抽出コード ここから
			Pattern pattern = Pattern
					.compile("(([あいうえお-ぢつ-もやゆよ-ろわをん][ぁぃぅぇぉゃゅょ]?){2})\\1|(([アイウエオ-ヂツ-モヤユヨ-ロワヲン][ァィゥェォャュョ]?){2})\\3");
			Matcher matcher = pattern.matcher(timeline);

			List<String> fuwafuwa = new ArrayList<String>();
			if (cache.get("fuwafuwa") != null) {
				fuwafuwa = (List<String>) cache.get("fuwafuwa");
			}

			String fwtemp;
			while (matcher.find()) {
				fwtemp = matcher.group();
				if (!fwtemp.matches("(.)\\1{3,3}")
						&& !fuwafuwa.contains(fwtemp)) {
					fuwafuwa.add(matcher.group());
				}
			}

			cache.put("fuwafuwa", fuwafuwa);
			// tsukasa code ここまで

			// URLエンコード
			timeline = URLEncoder.encode(timeline, "UTF-8");

			// Yahoo!キーフレーズ抽出APIをコール
			URL url = new URL(
					"http://jlp.yahooapis.jp/KeyphraseService/V1/extract");
			HttpURLConnection http = (HttpURLConnection) url.openConnection();

			http.setDoOutput(true);
			http.setUseCaches(false);
			http.setRequestMethod("POST");

			String postStr = "appid=YOUR_APPID_HERE&sentence="
					+ timeline;

			PrintWriter pw = new PrintWriter(http.getOutputStream());
			pw.print(postStr);
			pw.close();

			// XPathでパース
			DocumentBuilderFactory dbfactory = DocumentBuilderFactory
					.newInstance();
			XPathFactory factory = XPathFactory.newInstance();
			DocumentBuilder builder = dbfactory.newDocumentBuilder();
			InputStream is = http.getInputStream();
			Document doc = builder.parse(is);
			XPath xpath = factory.newXPath();

			Object result = xpath.evaluate("//Keyphrase/text()", doc,
					XPathConstants.NODESET);

			NodeList keyPhraseNodes = (NodeList) result;

			List<String> keyPhraseList = new ArrayList<String>();

			// メモリキャッシュからkeyPhraseListを取得
			if (cache.get("keyPhraseList") != null) {
				keyPhraseList = (List<String>) cache.get("keyPhraseList");
			}

			for (int i = 0; i < keyPhraseNodes.getLength(); i++) {
				if (keyPhraseList
						.indexOf(keyPhraseNodes.item(i).getNodeValue()) == -1) {
					if (keyPhraseNodes.item(i).getNodeValue().length() <= 65) {
						keyPhraseList
								.add(keyPhraseNodes.item(i).getNodeValue());
					}
				}
			}

			// Yahoo!形態素解析APIをコール
			url = new URL("http://jlp.yahooapis.jp/MAService/V1/parse");
			http = (HttpURLConnection) url.openConnection();

			http.setDoOutput(true);
			http.setUseCaches(false);
			http.setRequestMethod("POST");

			postStr = "appid=YOUR_APPID_HERE&sentence="
					+ timeline + "&results=ma&response=surface,pos";

			pw = new PrintWriter(http.getOutputStream());
			pw.print(postStr);
			pw.close();

			// XPathでパース
			is = http.getInputStream();
			doc = builder.parse(is);

			result = xpath.evaluate("//surface/text()", doc,
					XPathConstants.NODESET);
			Object posResult = xpath.evaluate("//pos/text()", doc,
					XPathConstants.NODESET);

			NodeList surfaceNodes = (NodeList) result;
			NodeList posNodes = (NodeList) posResult;

			// TODO tsukasaBot 形容詞抽出コード ここから
			List<String> adjectiveList = new ArrayList<String>();
			if (cache.get("adjectiveList") != null) {
				adjectiveList = (List<String>) cache.get("adjectiveList");
			}

			for (int i = 0; i < surfaceNodes.getLength(); i++) {
				if (posNodes.item(i).getNodeValue().equals("形容詞")) {
					if (surfaceNodes.item(i).getNodeValue().matches(".*い$")
							&& !adjectiveList.contains(surfaceNodes.item(i)
									.getNodeValue())) {
						adjectiveList.add(surfaceNodes.item(i).getNodeValue());
					}
				}
			}

			cache.put("adjectiveList", adjectiveList);
			// ここまで

			List<String> MAList = new ArrayList<String>();
			String tempPhrase = "";
			String reg;

			// メモリキャッシュからMAListを取得
			if (cache.get("MAList") != null) {
				MAList = (List<String>) cache.get("MAList");
			}

			boolean specialSurface = false;

			for (int i = 0; i < surfaceNodes.getLength(); i++) {
				if (tempPhrase.isEmpty()) {
					reg = "(感動詞|連体詞|接頭辞|名詞|特殊)";
				} else {
					reg = "(感動詞|接頭辞|接尾辞|名詞|特殊)";
				}

				if (tempPhrase.isEmpty()
						&& posNodes.item(i).getNodeValue().equals("特殊")) {
					specialSurface = true;
				} else if (specialSurface) {
					specialSurface = false;
					if (tempPhrase.trim().length() <= 1) {
						tempPhrase = "";
						continue;
					}
				}

				if (posNodes.item(i).getNodeValue().matches(reg)
						&& (!surfaceNodes.item(i).getNodeValue()
								.matches(".*[\"'\\[\\]{}:,.”’「」【】『』［］：、。，．].*") || specialSurface)) {
					tempPhrase += surfaceNodes.item(i).getNodeValue();
				} else {
					tempPhrase = tempPhrase.replaceAll("\\n", "").trim();
					if (!tempPhrase.isEmpty() && tempPhrase.length() <= 65) {
						if (MAList.indexOf(tempPhrase) == -1) {
							MAList.add(tempPhrase);
						}
						tempPhrase = "";
					}
				}
			}

			cache.put("sinceID", statuses.get(0).getId());
			cache.put("keyPhraseList", keyPhraseList);
			cache.put("MAList", MAList);
		} catch (TwitterException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			log.warning(e.getErrorMessage());
		} catch (ParserConfigurationException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			log.severe(e.getLocalizedMessage());
		} catch (SAXException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			log.severe(e.getLocalizedMessage());
		} catch (XPathExpressionException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			log.severe(e.getLocalizedMessage());
		}
	}
}
