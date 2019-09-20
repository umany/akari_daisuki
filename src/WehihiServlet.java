

import java.io.IOException;
import java.util.Random;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class WehihiServlet extends HttpServlet {

	final int max = 20;

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		Random random = new Random();
		String tweet = (random.nextInt(2) == 0) ? "ｳｪ" : "ﾃｨ";
		String text;
		int index = random.nextInt(4);
		int length;

		switch (index) {
		case 0:
			text = "ﾋ";
			break;
		case 1:
			text = "-";
			break;
		case 2:
			text = "...";
			break;
		default:
			text = "";
			break;
		}

		if (index != 3) {
			length = random.nextInt(max);
			for (int i = 0; i <= length; i++) {
				tweet += text;
			}
		}

		switch (index) {
		case 2:
			tweet += (random.nextInt(2) == 0) ? "ｳｪ" : "ﾃｨ";
		case 1:
			text = (random.nextInt(2) == 0 && index == 1) ? "ﾋ" : "-";
			length = random.nextInt(max);
			for (int i = 0; i <= length; i++) {
				tweet += text;
			}
		default:
			switch (random.nextInt(3)) {
			case 0:
				length = random.nextInt(max);
				for (int i = 0; i <= length; i++) {
					tweet += "!";

				}
				break;
			case 1:
				tweet += "?";
				break;
			default:
				break;
			}
		}

		TweetMadoka.Tweet(tweet);
		resp.setContentType("text/plain");
		resp.setCharacterEncoding("UTF-8");
		resp.getWriter().println(tweet);
	}
}
