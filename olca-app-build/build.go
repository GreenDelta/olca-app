package main

import (
	"archive/tar"
	"bufio"
	"io"
	"log"
	"os"
	"path/filepath"
	"strings"
)

func isDir(path ...string) bool {
	p := filepath.Join(path...)
	info, err := os.Stat(p)
	if err != nil {
		return false
	}
	return info.IsDir()
}

func cutdirs(path string, count int) string {
	if count < 1 {
		return path
	}
	sep := string(os.PathSeparator)
	raw := path
	if '/' != os.PathSeparator {
		raw = strings.Replace(raw, "/", sep, -1)
	}
	if '\\' != os.PathSeparator {
		raw = strings.Replace(raw, "\\", sep, -1)
	}
	parts := strings.Split(raw, string(os.PathSeparator))
	if count >= len(parts) {
		return parts[len(parts)-1]
	}
	return filepath.Join(parts[count:]...)
}

func untar(archive, folder string, cutdirCount int) error {
	if err := os.MkdirAll(folder, os.ModePerm); err != nil {
		return err
	}
	stream, err := os.Open(archive)
	defer stream.Close()
	if err != nil {
		return err
	}
	buffer := bufio.NewReader(stream)
	reader := tar.NewReader(buffer)
	for {
		header, err := reader.Next()
		if err == io.EOF {
			break
		}
		if err != nil {
			return err
		}
		path := filepath.Join(folder, cutdirs(header.Name, cutdirCount))
		info := header.FileInfo()
		if info.IsDir() {
			if err = os.MkdirAll(path, info.Mode()); err != nil {
				return err
			}
			continue
		}
		file, err := os.OpenFile(path, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, info.Mode())
		if err != nil {
			return err
		}
		defer file.Close()
		_, err = io.Copy(file, reader)
		if err != nil {
			return err
		}
	}
	return nil
}

func main() {
	log.SetOutput(os.Stdout)
	if !isDir("builds") {
		log.Fatalln("ERROR: no folder 'builds'")
	}

	jre := filepath.Join("runtime", "jre", "linux64", "jre-8u101-linux-x64.tar")
	dest := filepath.Join("builds", "linux.gtk.x86_64", "openLCA", "jre")
	err := untar(jre, dest, 1)
	if err != nil {
		log.Fatalln("Failed to untar JRE: ", err.Error())
	}
}
