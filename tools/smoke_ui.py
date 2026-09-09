"""Exercise the shared navigation and capture actual Android screenshots."""
import pathlib, re, subprocess, time, xml.etree.ElementTree as ET
out = pathlib.Path('build/ui'); out.mkdir(parents=True, exist_ok=True)
def adb(*args):
    return subprocess.check_output(['adb', *args], text=True).strip()
def capture(name):
    adb('shell', 'screencap', '-p', '/sdcard/ui.png')
    adb('pull', '/sdcard/ui.png', str(out / (name + '.png')))
def tap(label):
    adb('shell', 'uiautomator', 'dump', '/sdcard/ui.xml')
    adb('pull', '/sdcard/ui.xml', str(out / 'ui.xml'))
    root = ET.parse(out / 'ui.xml').getroot()
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
capture('01-month-dark')
tap('Năm'); capture('02-year-dark')
tap('Hôm nay'); capture('03-year-today')
tap('Tháng'); capture('04-month-return')
tap('Chọn ngày'); capture('05-date-picker'); adb('shell', 'input', 'keyevent', '4')
tap('Cài đặt'); capture('06-settings'); adb('shell', 'input', 'keyevent', '4')
adb('shell', 'cmd', 'uimode', 'night', 'no'); time.sleep(2)
capture('07-month-light'); tap('Năm'); capture('08-year-light')
assert adb('shell', 'pidof', 'com.dtinh.lichviet'), 'App process exited'
log = adb('logcat', '-d', '-b', 'crash')
(out / 'crash-log.txt').write_text(log)
assert 'FATAL EXCEPTION' not in log, log
print('Navigation smoke test passed; screenshots saved to build/ui')
