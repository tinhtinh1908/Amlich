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
def label(node):
    return node.get('content-desc') or node.get('text', '')
def assert_navigation(year):
    root = hierarchy()
    controls = {label(n) for n in root.iter('node') if n.get('clickable') == 'true'}
    assert {'Tháng', 'Năm', 'Cài đặt'} <= controls, controls
    assert ('Hôm nay' in controls) == (not year), controls
    assert ('Chọn năm' in controls) == year, controls
    if year:
        months = [n for n in root.iter('node') if 'Chạm để xem chi tiết' in n.get('content-desc','')]
        assert len(months) >= 6, 'Year grid missing'
def tap(target):
    root = hierarchy()
    for node in root.iter('node'):
        if label(node) == target and node.get('clickable') == 'true':
            x1,y1,x2,y2 = map(int, re.findall(r'\d+', node.get('bounds')))
            adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
            time.sleep(1)
            return
    raise AssertionError('Missing control: ' + target)
def check_notes():
    root = hierarchy()
    calendar = next(n for n in root.iter('node') if n.get('content-desc') == 'Lịch tháng Việt Nam')
    x1,y1,x2,y2 = map(int, re.findall(r'\d+', calendar.get('bounds')))
    density = int(re.findall(r'\d+', adb('shell', 'wm', 'density'))[-1]) / 160
    # Match the detail panel's adaptive spacing on tall displays.
    height_dp = (y2 - y1) / density
    grid_top_dp = 52 + min(40, (height_dp - 52) * 0.09)
    grid_space_dp = height_dp - 8 - 216 - 12 - grid_top_dp
    panel_inset_dp = min(56, max(0, grid_space_dp - 300) * 0.34)
    note_y = round(y2 - (8 + panel_inset_dp + 38) * density)
    adb('shell', 'input', 'tap', str((x1 + x2) // 2), str(note_y))
    time.sleep(1)
    texts = {n.get('text') for n in hierarchy().iter('node')}
    assert 'Ghi chú ngày' in texts, 'Note editor cannot be opened from the date detail'
    capture('01a-note-editor')
    adb('shell', 'input', 'keyevent', '4')
    if 'Ghi chú ngày' in {n.get('text') for n in hierarchy().iter('node')}:
        adb('shell', 'input', 'keyevent', '4')
adb('shell', 'cmd', 'uimode', 'night', 'yes')
adb('shell', 'am', 'start', '-W', '-n', 'com.dtinh.lichviet/.MainActivity')
time.sleep(3)
assert_navigation(False); capture('01-month-dark')
check_notes()
tap('Năm'); assert_navigation(True); capture('02-year-dark')
tap('Chọn năm'); capture('03-year-picker'); adb('shell', 'input', 'keyevent', '4')
tap('Tháng'); assert_navigation(False); capture('04-month-return')
tap('Hôm nay'); capture('05-month-today')
tap('Cài đặt'); capture('07-settings')
settings_text = {n.get('text') for n in hierarchy().iter('node')}
if not {'Ghi chú', 'Sao lưu'} <= settings_text:
    screen = hierarchy().find('.//node[@scrollable="true"]')
    assert screen is not None, 'Settings scroll view is missing'
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', screen.get('bounds')))
    x = str((x1 + x2) // 2)
    adb('shell', 'input', 'swipe', x, str(y1 + (y2 - y1) * 3 // 4),
        x, str(y1 + (y2 - y1) // 3), '350')
    settings_text = {n.get('text') for n in hierarchy().iter('node')}
assert {'Ghi chú', 'Sao lưu'} <= settings_text, 'Note backup setting is missing'
adb('shell', 'input', 'keyevent', '4')
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
