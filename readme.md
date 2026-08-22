# Lekki silnik 2D do gier RPG

## Opis projektu
Projekt polega na implementacji lekkiego silnika 2D przeznaczonego do gier RPG, który umożliwia proceduralną generację lochów oraz systemów zadań (questów). Kluczową cechą rozwiązania jest możliwość dostosowania generowanej zawartości do stylu gry użytkownika poprzez parametryzowane algorytmy.

Celem projektu jest stworzenie biblioteki programistycznej, która pozwala twórcom gier na elastyczne definiowanie mechanik generowania treści oraz ich optymalizację pod kątem wydajności i jakości rozgrywki.

## Zakres funkcjonalny
- Proceduralna generacja lochów
- Generacja questów reagująca na styl gry gracza
- Parametryzowalne algorytmy generowania treści
- Możliwość integracji z grami 2D
- Przykładowa gra demonstracyjna wykorzystująca silnik

## Technologie
Projekt wykorzystuje następujące technologie:
- Java 21
- gradle
- libgdx 1.14.2

## Struktura pracy z kodem
Kod źródłowy jest zarządzany w repozytorium GitHub z wykorzystaniem systemu gałęzi:
- `master` – stabilna wersja produkcyjna
- `dev` – główna gałąź rozwojowa
- `feature/*` – rozwój nowych funkcjonalności
- `hotfix` – poprawki błędów w wersji produkcyjnej

Konwencja commitów:
- `feat:` – nowe funkcjonalności
- `fix:` – poprawki błędów
- `docs:` – dokumentacja
- `chore:` – zmiany pomocnicze
- `style:` – zmiany stylistyczne
- `conf:` – konfiguracja
- `revert:` – cofnięcie zmian

## Etapy projektu
1. Analiza istniejących rozwiązań
2. Projekt architektury silnika i algorytmów generacji
3. Implementacja biblioteki
4. Testowanie i optymalizacja
5. Stworzenie gry demonstracyjnej
6. Testy użytkowników i zbieranie opinii
7. Dokumentacja
8. Prezentacja końcowa

## Zastosowanie
Projekt skierowany jest głównie do twórców niezależnych gier komputerowych (indie), którzy potrzebują elastycznego i lekkiego rozwiązania do generowania zawartości w grach RPG 2D.

## Termin realizacji
Planowane zakończenie projektu: styczeń/luty 2027
