"""Download the English Hayom Yom from Chabad.org (used with permission) into hayomyom-en.json.

Runs on GitHub's computers (see .github/workflows/hayomyom.yml), because Chabad.org can't be reached
from the editing workspace. Modes:
  probe  - fetch one day and print what the page looks like (to check the page layout)
  full   - fetch every Hebrew day of the year and write hayomyom-en.json
"""
import sys, re, json, time, html, urllib.request, datetime

UA = "Mozilla/5.0 (Linux; Android 14; Pixel 6 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36 CDate-calendar (permission granted)"
BASE = "https://www.chabad.org/dailystudy/hayomyom.asp?tdate={m}/{d}/{y}"

def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "text/html", "Accept-Language": "en"})
    with urllib.request.urlopen(req, timeout=40) as r:
        return r.status, r.geturl(), r.read().decode("utf-8", "replace")

def probe():
    day = datetime.date.today()
    url = BASE.format(m=day.month, d=day.day, y=day.year)
    st, final, page = get(url)
    print("STATUS", st, "FINAL", final, "LENGTH", len(page))
    body = re.sub(r"(?is)<(script|style|noscript|svg)[^>]*>.*?</\1>", "", page)
    # print the region with the most paragraph text, with its tags, so the layout can be read
    i = max(range(0, max(1, len(body) - 6000), 500), key=lambda k: len(re.findall(r"(?i)<p[ >]", body[k:k + 6000])))
    print("----- TITLE:", re.search(r"(?is)<title>(.*?)</title>", page).group(1).strip() if "<title" in page.lower() else "")
    for m in re.finditer(r'(?i)class="([^"]*(?:article|body|content|hayom|text)[^"]*)"', body):
        print("CLASS", m.group(1))
    print("----- DENSEST REGION -----")
    print(re.sub(r"\n\s*\n+", "\n", body[i:i + 6000]))

if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "probe"
    if mode == "probe":
        probe()
    else:
        print("full mode not written yet")
