

import java.util.logging.Logger;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TweetMadoka {

	private static final Logger log = Logger.getLogger(TweetMadoka.class
			.getName());

	public static void Tweet(String status) {
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
			twitter.updateStatus(status);
		} catch (TwitterException e) {
			log.warning("failed to tweet. " + e.getErrorMessage());
		}
	}
}
