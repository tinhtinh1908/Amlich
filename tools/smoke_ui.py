"""Exercise the shared navigation and capture actual Android screenshots."""
import pathlib, re, subprocess, time, xml.etree.ElementTree as ET
out = pathlib.Path('build/ui'); out.mkdir(parents=True, exist_ok=True)
def adb(*args):
    return subprocess.check_output(['adb', *args], text=True).strip()
def capture(name):
    adb('shell', 'screencap', '-p', '/sdcard/ui.png')
    adb('pull', '/sdcard/ui.png', str(out / (name + '.png')))
def hierarchy():
    adb('shell', 'uiautomator', 'dump', '/sdcard/ui.xml')
    adb('pull', '/sdcard/ui.xml', str(out / 'ui.xml'))
    return ET.parse(out / 'ui.xml').getroot()
def assert_navigation(year):
    labels = {'Chọn ngày', 'Chọn năm', 'Năm', 'Tháng', 'Hôm nay', 'Cài đặt'}
    actual = [n.get('content-desc') for n in hierarchy().iter('node')
              if n.get('clickable') == 'true' and n.get('content-desc') in labels]
    expected = ['Chọn năm', 'Tháng', 'Cài đặt'] if year else ['Chọn ngày', 'Năm', 'Hôm nay', 'Cài đặt']
    assert actual == expected, (actual, expected)
def tap(label):
    root = hierarchy()
    for node in root.iter('node'):
        if node.get('content-desc') == label and node.get('clickable') == 'true':
            x1,y1,x2,y2 = map(int, re.findall(r'\d+', node.get('bounds')))
            adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
            time.sleep(1)
            return
    raise AssertionError('Missing control: ' + label)
adb('shell', 'cmd', 'uimode', 'night', 'yes')
adb('shell', 'am', 'start', '-W', '-n', 'com.dtinh.lichviet/.MainActivity')
time.sleep(3)
assert_navigation(False); capture('01-month-dark')
tap('Năm'); assert_navigation(True); capture('02-year-dark')
tap('Chọn năm'); capture('03-year-picker'); adb('shell', 'input', 'keyevent', '4')
tap('Tháng'); assert_navigation(False); capture('04-month-return')
tap('Hôm nay'); capture('05-month-today')
tap('Chọn ngày'); capture('06-date-picker'); adb('shell', 'input', 'keyevent', '4')
tap('Cài đặt'); capture('07-settings'); adb('shell', 'input', 'keyevent', '4')
adb('shell', 'cmd', 'uimode', 'night', 'no'); time.sleep(2)
assert_navigation(False); capture('08-month-light')
tap('Năm'); assert_navigation(True); capture('09-year-light')
adb('shell', 'cmd', 'uimode', 'night', 'yes'); time.sleep(2)
assert_navigation(True)
tap('Tháng'); assert_navigation(False)
assert adb('shell', 'pidof', 'com.dtinh.lichviet'), 'App process exited'
log = adb('logcat', '-d', '-b', 'crash')
(out / 'crash-log.txt').write_text(log)
assert 'FATAL EXCEPTION' not in log, log
print('Navigation smoke test passed; screenshots saved to build/ui')
