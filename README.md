# 🛠️ AntToMavenConverter

**AntToMavenConverter** is a tool that helps you migrate Java NetBeans projects from **Apache Ant** to **Maven** structure with ease.

---

## 🚀 How to Use AntToMavenConverter

### ✅ Prerequisites

- Make sure **Java is installed** (Java 8 or newer).
- Verify by running in Command Prompt:
  ```bash
  java -version
  ```

---

### 📦 Step-by-Step Instructions

1. **Download these two files** from the [Releases](https://github.com/your-username/AntToMavenConverter/releases):
   - `AntToMavenConverter.jar`
   - `StartAntToMavenConverter.bat`

2. **Place both files in the same folder**.

3. **Double-click** `StartAntToMavenConverter.bat` to launch the tool.

4. In the graphical window:
   - Click **"Select Project Folder"** and choose your **NetBeans Ant project folder**.
   - Click **"Convert to Maven"**.

---

### 📁 Where to Find the Output

- The converted Maven project will be created **next to your original project folder**, with `-mavenized` added to its name.

  **Example**:  
  If your project folder is:
  ```
  D:\Projects\StudentPortal
  ```
  The output will be:
  ```
  D:\Projects\StudentPortal-mavenized
  ```

---

### ✅ What’s Inside the Converted Project

- `pom.xml` with basic dependencies
- `src/main/java` (Java source files)
- `src/main/webapp` (web resources like JSP)
- Copied `lib/` folder if available
- WAR packaging support

---

### 📄 License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.
