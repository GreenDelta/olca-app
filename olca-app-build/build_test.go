package main

import (
	"path/filepath"
	"testing"
)

func TestCutdirs(t *testing.T) {
	in := filepath.Join("a", "b", "c", "d")
	out := cutdirs(in, 0)
	if out != in {
		t.Fatal("expected a/b/c/d but was:", out)
	}
	out = cutdirs(in, 1)
	if out != filepath.Join("b", "c", "d") {
		t.Fatal("expected b/c/d but was:", out)
	}
	out = cutdirs(in, 2)
	if out != filepath.Join("c", "d") {
		t.Fatal("expected c/d but was:", out)
	}
	for i := 3; i < 6; i++ {
		out = cutdirs(in, i)
		if out != "d" {
			t.Fatal(i, "expected d but was:", out)
		}
	}
}
