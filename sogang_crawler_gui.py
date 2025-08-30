import tkinter as tk
from tkinter import ttk, scrolledtext
import threading
import time
import os
import sys
import glob
import pandas as pd
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from webdriver_manager.chrome import ChromeDriverManager

# --- Crawler Core Logic ---
def run_crawler(year, semester, download_path, log_callback):
    """Function to run the Selenium web crawler"""
    driver = None

    # 1. Define target file name
    target_base_name = "{}_year_{}".format(year, semester.replace(" ", "_"))
    target_csv_path = os.path.join(download_path, target_base_name + '.csv')
    target_html_path = os.path.join(download_path, target_base_name + '.html')

    # 2. Before starting, check if the final CSV file already exists
    if os.path.exists(target_csv_path):
        log_callback("File '{}' already exists. Skipping task.".format(target_csv_path))
        log_callback("\n--- Task Complete ---")
        return

    # 3. Check for previously downloaded html(xls) file
    if os.path.exists(target_html_path):
        log_callback("Found existing downloaded file '{}'. Starting conversion immediately.".format(os.path.basename(target_html_path)))
        convert_html_to_csv(target_html_path, target_csv_path, log_callback)
        log_callback("\n--- Task Complete ---")
        return

    # 4. If none of the above, start web crawling
    try:
        log_callback("Starting crawler for {} year {}.".format(year, semester))

        # Chrome options setup
        chrome_options = Options()
        prefs = {
            "download.default_directory": download_path,
            "download.prompt_for_download": False,
            "download.directory_upgrade": True,
            "safebrowsing.enabled": True
        }
        chrome_options.add_experimental_option("prefs", prefs)
        chrome_options.add_argument("--headless")
        chrome_options.add_argument("--disable-gpu")
        chrome_options.add_argument("--window-size=1920x1080")

        # Web driver setup
        service = Service(ChromeDriverManager().install())
        driver = webdriver.Chrome(service=service, options=chrome_options)
        wait = WebDriverWait(driver, 15)

        # Accessing website
        log_callback("Connecting to the website...")
        url = "https://sis109.sogang.ac.kr/sap/bc/webdynpro/sap/zcmw9016?sap-language=KO"
        driver.get(url)
        time.sleep(2)

        # Dropdown menu selection function
        def select_dropdown_option(trigger_id, option_text):
            log_callback("Selecting item: {}".format(option_text))
            trigger_element = wait.until(EC.element_to_be_clickable((By.ID, trigger_id)))
            trigger_element.click()
            time.sleep(0.5)
            option_xpath = "//div[contains(@class, 'lsListbox__value') and text()='{}']".format(option_text)
            option_element = wait.until(EC.element_to_be_clickable((By.XPATH, option_xpath)))
            option_element.click()
            log_callback("{} selected. Waiting for page to stabilize...".format(option_text))
            time.sleep(1.5)

        # Select year, semester, affiliation
        select_dropdown_option("WD2A", "{} 학년도".format(year))
        select_dropdown_option("WD4F", semester)
        select_dropdown_option("WD83", "대학")

        # Click search button
        log_callback("Clicking the search button.")
        search_button = wait.until(EC.element_to_be_clickable((By.ID, "WDC1")))
        search_button.click()

        # Wait for data to load
        log_callback("Waiting 30 seconds for data to load...")
        time.sleep(30)

        # Click download button
        log_callback("Clicking the download button.")
        download_button = wait.until(EC.element_to_be_clickable((By.ID, "WDC2")))
        download_button.click()

        # Wait for download to complete
        log_callback("Waiting 30 seconds for the download to complete.")
        time.sleep(30)

        # Find the downloaded file, rename it, and convert to CSV
        convert_downloaded_file(download_path, target_html_path, target_csv_path, log_callback)

        # Verify that the final CSV file was created
        if os.path.exists(target_csv_path):
            log_callback("\n--- Task Complete ---")
            log_callback("Successfully created CSV file. Saved at: {}".format(target_csv_path))
        else:
            log_callback("\n--- Task Failed ---")
            log_callback("Failed to create the final CSV file. Please check intermediate files and logs.")

    except Exception as e:
        log_callback("\n--- Error Occurred ---")
        log_callback("An error occurred: {}".format(e))
    finally:
        if driver:
            driver.quit()
            log_callback("Browser has been closed.")

# --- Downloaded File Conversion Logic ---
def convert_downloaded_file(download_path, target_html_path, target_csv_path, log_callback):
    """Finds '개설교과목정보.xls', renames it, and converts to CSV"""
    original_file_path = os.path.join(download_path, '개설교과목정보.xls')
    
    timeout = 20
    while not os.path.exists(original_file_path) and timeout > 0:
        time.sleep(1)
        timeout -= 1
    
    if not os.path.exists(original_file_path):
        log_callback("Error: Downloaded file '개설교과목정보.xls' not found.")
        return

    log_callback("Found downloaded file '개설교과목정보.xls'.")
    
    # Rename
    if os.path.exists(target_html_path):
        os.remove(target_html_path)
    os.rename(original_file_path, target_html_path)
    log_callback("Renamed file to '{}'.".format(os.path.basename(target_html_path)))

    # Convert to CSV
    convert_html_to_csv(target_html_path, target_csv_path, log_callback)

# --- HTML to CSV Conversion Function ---
def convert_html_to_csv(html_path, csv_path, log_callback):
    """Reads an HTML file, saves it as a CSV file, and deletes the original HTML."""
    try:
        log_callback("Starting HTML to CSV conversion...")
        df = None
        try:
            dataframes = pd.read_html(html_path, encoding='utf-8')
            df = dataframes[0]
            log_callback("Successfully read the file with UTF-8 encoding.")
        except Exception:
            log_callback("UTF-8 read failed. Retrying with CP949 encoding...")
            dataframes = pd.read_html(html_path, encoding='cp949')
            df = dataframes[0]
            log_callback("Successfully read the file with CP949 encoding.")

        # Save to CSV file
        df.to_csv(csv_path, index=False, encoding='utf-8-sig')
        log_callback("Success: Saved as '{}'வுகளை.".format(csv_path))
        
        # Delete the intermediate HTML file
        os.remove(html_path)
        log_callback("Deleted intermediate file '{}'வுகளை.".format(os.path.basename(html_path)))

    except Exception as e:
        log_callback("An error occurred during CSV conversion: {}".format(e))

# --- GUI Application ---
class CrawlerApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Sogang University Course Catalog Crawler")
        self.root.geometry("450x400")

        # Frame setup
        frame = ttk.Frame(root, padding="10")
        frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))

        # Widget creation
        ttk.Label(frame, text="Year:").grid(column=0, row=0, sticky=tk.W, pady=5)
        self.year_entry = ttk.Entry(frame, width=15)
        self.year_entry.insert(0, "2025") # Default value
        self.year_entry.grid(column=1, row=0, sticky=tk.W, pady=5)

        ttk.Label(frame, text="Semester:").grid(column=0, row=1, sticky=tk.W, pady=5)
        self.semester_combo = ttk.Combobox(frame, values=["1학기", "2학기", "하계학기", "동계학기"], width=12)
        self.semester_combo.current(1) # Set '2학기' as default
        self.semester_combo.grid(column=1, row=1, sticky=tk.W, pady=5)

        self.crawl_button = ttk.Button(frame, text="Start Crawling", command=self.start_crawling)
        self.crawl_button.grid(column=0, row=2, columnspan=2, pady=15)

        ttk.Label(frame, text="Progress:").grid(column=0, row=3, sticky=tk.W, pady=5)
        self.log_text = scrolledtext.ScrolledText(frame, width=50, height=15, wrap=tk.WORD)
        self.log_text.grid(column=0, row=4, columnspan=2, sticky=(tk.W, tk.E))

    def log(self, message):
        """Adds a log message to the text widget"""
        self.log_text.insert(tk.END, message + "\n")
        self.log_text.see(tk.END)

    def start_crawling(self):
        """Function to be called when the crawl button is clicked"""
        year = self.year_entry.get()
        semester = self.semester_combo.get()
        download_path = os.getcwd() # In GUI mode, download to the current directory

        if not year.isdigit() or len(year) != 4:
            self.log("Error: Please enter a valid 4-digit year.")
            return

        self.crawl_button.config(state="disabled")
        self.log_text.delete('1.0', tk.END)

        # Use a thread to prevent the GUI from freezing
        thread = threading.Thread(
            target=run_crawler,
            args=(year, semester, download_path, self.log_in_thread),
            daemon=True
        )
        thread.start()

    def log_in_thread(self, message):
        """Function to safely update the GUI from a thread"""
        self.root.after(0, self.log, message)
        if "Task Complete" in message or "Error Occurred" in message:
            self.root.after(0, self.enable_button)

    def enable_button(self):
        """Re-enables the button"""
        self.crawl_button.config(state="normal")

if __name__ == "__main__":
    # If there are 4 command-line arguments (script_name, year, semester, download_path)
    if len(sys.argv) == 4:
        cmd_year = sys.argv[1]
        cmd_semester = sys.argv[2]
        cmd_download_path = sys.argv[3]
        
        # Use the print function as a callback for server logs
        run_crawler(cmd_year, cmd_semester, cmd_download_path, print)
    # If no arguments, run the GUI
    else:
        root = tk.Tk()
        app = CrawlerApp(root)
        root.mainloop()