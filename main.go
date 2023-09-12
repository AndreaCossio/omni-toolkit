package main

import (
	"bufio"
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"text/template"

	cp "github.com/otiai10/copy"
)

const (
	all                 = "all"
	reset               = "reset"
	config              = "config"
	build               = "build"
	deploy              = "deploy"
	doc                 = "doc"
	tmplAppianPluginXml = "./templates/appian-plugin.xml"
	tmplEnUsProperties  = "./templates/_en_US.properties"
	tmplDocumentation   = "./templates/documentation.html"
	outAppianPluginXml  = "./out/appian-plugin.xml"
)

var (
	ConfigMap    map[string]interface{}
	JarFile      string
	DeployUrl    string
	DeployApiKey string
	ok           bool
)

func init() {
	configJson, err := os.Open("config.json")
	if err != nil {
		log.Println(err.Error())
	}
	defer configJson.Close()
	b, _ := io.ReadAll(configJson)
	json.Unmarshal([]byte(b), &ConfigMap)
	JarFile, ok = ConfigMap["JarFile"].(string)
	if !ok {
		log.Println("Error JarFile")
	}
	deployMap, ok := ConfigMap["Deploy"].(map[string]interface{})
	if !ok {
		log.Println("Error Deploy")
	}
	DeployUrl, ok = deployMap["Url"].(string)
	if !ok {
		log.Println("Error Deploy Url")
	}
	DeployApiKey, ok = deployMap["ApiKey"].(string)
	if !ok {
		log.Println("Error Deploy ApiKey")
	}
}

func main() {
	args := os.Args[1:]
	log.Println("ARGS", args)

fast:
	for _, arg := range args {
		switch arg {

		case all:
			resetFunc()
			configFunc()
			buildFunc()
			docFunc()
			deployFunc()

			// I eat bugs for
			break fast

		case reset:
			resetFunc()

		case config:
			configFunc()

		case build:
			buildFunc()

		case deploy:
			deployFunc()

		case doc:
			docFunc()

		default:
			log.Println(arg)
		}
	}

}

func resetFunc() {
	os.RemoveAll("out")
	os.Mkdir("out", os.ModePerm)
}

func configFunc() {
	var pluginKey = ConfigMap["Key"].(string)
	resourcesFolder := "out/" + strings.ReplaceAll(pluginKey, ".", "/")
	os.RemoveAll(resourcesFolder)
	os.MkdirAll(resourcesFolder, os.ModePerm)

	// appian-plugin.xml
	createFileFromTemplate(tmplAppianPluginXml, outAppianPluginXml, ConfigMap)

	// functions properties files
	for _, function := range ConfigMap["Functions"].([]interface{}) {
		functionMap, _ := function.(map[string]interface{})
		key := functionMap["Key"].(string)
		outputFile := resourcesFolder + "/" + key + "_en_US.properties"
		createFileFromTemplate(
			tmplEnUsProperties,
			outputFile,
			map[string]any{
				"Function": function,
			},
		)
	}

	// smart services properties files
	for _, smartService := range ConfigMap["SmartServices"].([]interface{}) {
		smartServiceMap, _ := smartService.(map[string]interface{})
		key := smartServiceMap["Key"].(string)
		outputFile := resourcesFolder + "/" + key + "_en_US.properties"
		createFileFromTemplate(
			tmplEnUsProperties,
			outputFile,
			map[string]any{
				"SmartService": smartService,
			},
		)
	}

	// connected systems properties files (unique containing all entities)
	connectedSystemCount := len(ConfigMap["ConnectedSystems"].([]interface{}))
	if connectedSystemCount > 0 {
		outputFile := "out/resources_en_US.properties"
		createFileFromTemplate(
			tmplEnUsProperties,
			outputFile,
			map[string]any{
				"ConnectedSystem": ConfigMap,
			},
		)
	}

	// function category
	for _, functionCategory := range ConfigMap["FunctionCategories"].([]interface{}) {
		functionCategoryMap, _ := functionCategory.(map[string]interface{})
		key := functionCategoryMap["Key"].(string)
		outputFile := resourcesFolder + "/" + key + "_en_US.properties"
		createFileFromTemplate(
			tmplEnUsProperties,
			outputFile,
			map[string]any{
				"FunctionCategory": functionCategory,
			},
		)
	}

}

func javaFilesList(dir string, out string) {
	f, err := os.OpenFile(out, os.O_RDWR|os.O_CREATE|os.O_TRUNC, 0755)
	if err != nil {
		log.Fatal(err)
	}

	defer f.Close()

	err2 := filepath.Walk(dir, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		if filepath.Ext(path) == ".java" {
			_, err2 := f.WriteString(path + "\n")
			if err2 != nil {
				log.Fatal(err2)
			}
		}
		return nil
	})
	if err2 != nil {
		log.Fatal(err2)
	}
}

func buildFunc() {
	// Build missing files
	javaFilesList("./missing", "missingJavaList.txt")
	out, _ := exec.Command("javac", "-cp", "./libs/*", "@missingJavaList.txt").Output()
	fmt.Printf("%s", out)
	os.Remove("missingJavaList.txt")
	out2, _ := exec.Command("jar", "cvf", "libs/missing.jar", "-C", "missing", ".").Output()
	fmt.Printf("%s", out2)

	// Build all
	javaFilesList("./java", "tmpJavaList.txt")
	out3, _ := exec.Command("javac", "-cp", "./libs/*", "-d", "./out", "@tmpJavaList.txt").Output()
	fmt.Printf("%s", out3)
	cp.Copy("./java", "./out/src")
	out5, _ := exec.Command("jar", "cvf", JarFile, "-C", "out", ".").Output()
	fmt.Printf("%s", out5)
	os.Remove("tmpJavaList.txt")
}

func deployFunc() {
	// Set your target URL
	url := DeployUrl

	// Create a new HTTP request
	b, err := os.ReadFile(JarFile)
	if err != nil {
		return
	}
	req, err := http.NewRequest("POST", url, bytes.NewReader(b))
	if err != nil {
		fmt.Println("Error creating request:", err)
		return
	}

	// Set headers for the request
	req.Header.Set("Content-Type", "application/octet-stream")
	req.Header.Set("Appian-API-Key", DeployApiKey)
	req.Header.Set("Appian-Document-Name", JarFile)

	// Make the HTTP request
	client := http.DefaultClient
	resp, err := client.Do(req)
	if err != nil {
		fmt.Println("Error sending request:", err)
		return
	}
	defer resp.Body.Close()

	// Process the response
	fmt.Println("Response Status:", resp.Status)
}

func docFunc() {
	outputFile := "out/documentation.html"
	createFileFromTemplate(
		tmplDocumentation,
		outputFile,
		ConfigMap,
	)
}

func createFileFromTemplate(templateFile string, outputFile string, tmpData any) error {
	tmpl := template.Must(template.New(templateFile).ParseFiles(templateFile))
	var processed bytes.Buffer
	err := tmpl.ExecuteTemplate(&processed, templateFile, tmpData)
	if err != nil {
		log.Println(err.Error())
		return err
	}
	f, err := os.Create(outputFile)
	if err != nil {
		log.Println(err.Error())
		return err
	}
	w := bufio.NewWriter(f)
	_, err = w.WriteString(processed.String())
	if err != nil {
		log.Println(err.Error())
		return err
	}
	err = w.Flush()
	if err != nil {
		log.Println(err.Error())
		return err
	}
	err = f.Close()
	if err != nil {
		log.Println(err.Error())
		return err
	}
	return nil
}
