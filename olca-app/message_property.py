import string
from datetime import datetime

M_FILE = 'src/org/openlca/app/M.java'
LANGUAGES = ['', 'ar', 'bg', 'ca', 'de', 'es', 'fr', 'hu', 'it', 'pt', 'tr', 'zh_cn', 'zh_tw']


def prop_file(language: str) -> str:
    suffix = '_' + language if language != '' else ''
    return f'src/org/openlca/app/messages{suffix}.properties'


def remove_unused():
    """
    Removes the unused properties from the messages[...].properties file.
    """
    messages = get_messages()

    def write():
        date = datetime.now().strftime("#%a %b %d %H:%M:%S CET %Y\n")
        f.write(date)
        for line in sorted(lines[1:]):
            message = line[:line.index('=')]
            if message in messages:
                f.write(line)

    for lang in LANGUAGES:
        with open(prop_file(lang)) as f:
            lines = f.readlines()
        with open(prop_file(lang), 'w') as f:
            write()


def sort_m_file():
    """
    Prints the messages in the M.java file in alphabetical order.
    """
    messages = get_messages()

    # print the lines if necessary.
    sorted_messages = sorted(messages)
    if messages == sorted_messages:
        letters = string.ascii_uppercase
        print('\n// A')
        i = 0
        for m in sorted(messages):
            while m[0] != letters[i]:
                i += 1
                print('\n// ' + letters[i])
            print('	public static String ' + m + ';')


def get_messages() -> list[str]:
    """
    Returns the list of messages in the M.java file.
    """
    messages = []
    with open(M_FILE) as f:
        lines = f.readlines()
        prefix = '	public static String '
        suffix = ';\n'

        for line in lines:
            if line.startswith(prefix) and line.endswith(suffix):
                messages.append(line[len(prefix):-len(suffix)])

    return messages


def main():
    sort_m_file()
    remove_unused()


if __name__ == '__main__':
    main()
