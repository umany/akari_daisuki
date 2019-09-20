

/**
 * つかさbotツイート用クラス
 * 超やっつけコーディングバージョン
 * @author umany
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.cache.Cache;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

@SuppressWarnings("serial")
public class TsukasaTweetServlet extends HttpServlet {

	@SuppressWarnings("unchecked")
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		// create config for @TsukasaDaisuki
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

		String something;
		String adjective;
		String fuwafuwa;

		List<String> keyPhraseList = new ArrayList<String>();
		List<String> adjectiveList = new ArrayList<String>();
		List<String> fwList = new ArrayList<String>();

		Random random = new Random();

		Cache cache = AkariUtil.getCache();

		if (cache.get("keyPhraseList") != null) {
			keyPhraseList = (List<String>) cache.get("keyPhraseList");
		} else {
			return;
		}
		if (cache.get("adjectiveList") != null) {
			adjectiveList = (List<String>) cache.get("adjectiveList");
		} else {
			return;
		}
		if (cache.get("fuwafuwa") != null) {
			fwList = (List<String>) cache.get("fuwafuwa");
		} else {
			return;
		}

		something = keyPhraseList.get(random.nextInt(keyPhraseList.size()));
		adjective = adjectiveList.get(random.nextInt(adjectiveList.size()));
		fuwafuwa = fwList.get(random.nextInt(fwList.size()));

		try {
			twitter.updateStatus(something + "って" + fuwafuwa + "で" + adjective
					+ "から大好き〜");
		} catch (TwitterException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

}
