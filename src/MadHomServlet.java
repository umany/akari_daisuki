

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.htmlparser.jericho.Source;

@SuppressWarnings("serial")
public class MadHomServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(MadHomServlet.class
			.getName());

	public void doGet(HttpServletRequest req, HttpServletResponse resp) {
		try {
			URL url = new URL("https://shindanmaker.com/280267");
			HttpURLConnection http = (HttpURLConnection) url.openConnection();

			http.setDoOutput(true);
			http.setUseCaches(false);
			http.setRequestMethod("POST");

			String postStr = "u=wehihi_thihihi";

			PrintWriter pw = new PrintWriter(http.getOutputStream());
			pw.print(postStr);
			pw.close();

			Source source = new Source(http.getInputStream());
			String result = source.getElementById("copy_text_140").getTextExtractor()
					.toString();

			// String result;
			// BufferedReader reader = new BufferedReader(new InputStreamReader(
			// http.getInputStream(), "UTF-8"/* 文字コード指定 */));
			// StringBuffer buf = new StringBuffer();
			// while ((result = reader.readLine()) != null) {
			// buf.append(result);
			// buf.append("\n");
			// }

			TweetMadoka.Tweet(result);
			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");
			resp.getWriter().println(result);
		} catch (Exception e) {
			log.warning("failed to fetch shindanmaker." + e.getMessage());
		}
	}
}
